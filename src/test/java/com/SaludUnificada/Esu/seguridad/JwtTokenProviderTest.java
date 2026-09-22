package com.SaludUnificada.Esu.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "MiClaveSecretaSuperSeguraParaElEcosistemaDeSaludUnificadoEsu2026");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3600000L);
    }

    @Test
    void testGenerarYValidarToken() {
        String email = "paciente@ejemplo.com";
        String token = jwtTokenProvider.generarTokenParaUsuario(email, "PACIENTE");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validarToken(token));
        assertEquals(email, jwtTokenProvider.obtenerUsernameDelJwt(token));
    }

    @Test
    void testTokenInvalido() {
        assertFalse(jwtTokenProvider.validarToken("token_invalido_totalmente"));
    }
}
