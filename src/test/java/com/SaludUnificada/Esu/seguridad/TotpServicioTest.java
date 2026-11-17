package com.SaludUnificada.Esu.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TotpServicioTest {

    private TotpServicio totpServicio;

    @BeforeEach
    void setUp() {
        totpServicio = new TotpServicio();
    }

    @Test
    @DisplayName("Generar secreto: genera clave base32 de longitud esperada")
    void generarSecreto_longitudValida() {
        String secreto = totpServicio.generarSecreto();
        assertNotNull(secreto);
        assertEquals(32, secreto.length());
        assertTrue(secreto.matches("^[A-Z2-7]+$"));
    }

    @Test
    @DisplayName("Generar códigos de respaldo: produce 10 códigos numéricos de 8 dígitos")
    void generarCodigosRespaldo_diezCodigosOchoDigitos() {
        List<String> codigos = totpServicio.generarCodigosRespaldo();
        assertNotNull(codigos);
        assertEquals(10, codigos.size());
        for (String c : codigos) {
            assertEquals(8, c.length());
            assertTrue(c.matches("^\\d{8}$"));
        }
    }

    @Test
    @DisplayName("Generar URI y QR data URL: produce estructura otpauth válida y base64 PNG")
    void generarOtpauthUriYQr_valido() {
        String secreto = totpServicio.generarSecreto();
        String uri = totpServicio.generarOtpauthUri(secreto, "usuario@esu.gob.ar");
        assertNotNull(uri);
        assertTrue(uri.startsWith("otpauth://totp/ESU:usuario@esu.gob.ar?secret="));

        String qr = totpServicio.generarQrDataUrl(uri);
        assertNotNull(qr);
        assertTrue(qr.startsWith("data:image/png;base64,"));
    }

    @Test
    @DisplayName("Anti-replay TOTP: el mismo código no puede reutilizarse en la misma ventana")
    void validarCodigo_antiReplay_rechazaMismoContador() {
        String secreto = totpServicio.generarSecreto();

        // Extraer código válido para el instante actual usando reflexión o validación
        long ahora = System.currentTimeMillis() / 1000 / 30;
        // Generar código válido
        byte[] clave = org.springframework.test.util.ReflectionTestUtils.invokeMethod(totpServicio, "decodificarBase32", secreto);
        String codigoValido = org.springframework.test.util.ReflectionTestUtils.invokeMethod(totpServicio, "generarCodigo", clave, ahora);

        // Primer uso: sin contador previo -> Debe ser válido y retornar el contador
        long primerContador = totpServicio.validarCodigo(secreto, codigoValido, null);
        assertTrue(primerContador >= ahora - 1 && primerContador <= ahora + 1);

        // Segundo uso (Replay attack): con ultimoContador = primerContador -> DEBE RECHAZARSE (-1)
        long replayContador = totpServicio.validarCodigo(secreto, codigoValido, primerContador);
        assertEquals(-1L, replayContador, "El código TOTP ya utilizado debe ser rechazado por control anti-replay.");
    }

    @Test
    @DisplayName("Código inválido: formato erróneo o dígito incorrecto retorna -1")
    void validarCodigo_codigoInvalido_retornaMenosUno() {
        String secreto = totpServicio.generarSecreto();
        assertEquals(-1L, totpServicio.validarCodigo(secreto, "abc", null));
        assertEquals(-1L, totpServicio.validarCodigo(secreto, "12345", null));
        assertEquals(-1L, totpServicio.validarCodigo(secreto, "000000", null));
    }
}
