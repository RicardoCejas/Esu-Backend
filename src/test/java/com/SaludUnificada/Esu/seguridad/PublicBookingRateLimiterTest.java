package com.SaludUnificada.Esu.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PublicBookingRateLimiterTest {

    private RequestIpUtil requestIpUtil;
    private PublicBookingRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        requestIpUtil = new RequestIpUtil("127.0.0.1,10.0.0.1");
        // Ventana corta para testing: 100 ms y máximo 3 solicitudes
        rateLimiter = new PublicBookingRateLimiter(requestIpUtil, 100L, 3);
    }

    @Test
    @DisplayName("Debe permitir solicitudes hasta el límite y bloquear la siguiente")
    void respetaLimiteMaximoPorVentana() {
        String ip = "192.168.1.50";

        assertTrue(rateLimiter.permitirReserva(ip), "Petición 1 permitida");
        assertTrue(rateLimiter.permitirReserva(ip), "Petición 2 permitida");
        assertTrue(rateLimiter.permitirReserva(ip), "Petición 3 permitida (límite)");
        assertFalse(rateLimiter.permitirReserva(ip), "Petición 4 bloqueada por rate limit");
    }

    @Test
    @DisplayName("Al vencer la ventana debe reiniciar ventana y contador permitiendo nuevas solicitudes")
    void reiniciaVentanaYContadorAlVencer() throws InterruptedException {
        String ip = "192.168.1.60";

        assertTrue(rateLimiter.permitirReserva(ip));
        assertTrue(rateLimiter.permitirReserva(ip));
        assertTrue(rateLimiter.permitirReserva(ip));
        assertFalse(rateLimiter.permitirReserva(ip), "Bloqueado en ventana actual");

        // Esperar a que venza la ventana configurada (100 ms)
        Thread.sleep(120);

        assertTrue(rateLimiter.permitirReserva(ip), "Debe permitir petición tras reset de ventana");
        assertTrue(rateLimiter.permitirReserva(ip), "Contador reiniciado: petición 2 permitida");
        assertTrue(rateLimiter.permitirReserva(ip), "Contador reiniciado: petición 3 permitida");
        assertFalse(rateLimiter.permitirReserva(ip), "Bloqueado nuevamente al alcanzar el límite");
    }

    @Test
    @DisplayName("Acepta IP reenviada de X-Forwarded-For solo si proviene de proxy confiable")
    void aceptaXForwardedForSoloDesdeProxyConfiable() {
        HttpServletRequest reqConfiable = Mockito.mock(HttpServletRequest.class);
        when(reqConfiable.getRemoteAddr()).thenReturn("10.0.0.1"); // Proxy confiable
        when(reqConfiable.getHeader("X-Forwarded-For")).thenReturn("200.50.10.5, 10.0.0.1");

        HttpServletRequest reqNoConfiable = Mockito.mock(HttpServletRequest.class);
        when(reqNoConfiable.getRemoteAddr()).thenReturn("192.168.1.100"); // No es proxy confiable
        when(reqNoConfiable.getHeader("X-Forwarded-For")).thenReturn("200.50.10.5"); // Intento de spoofing

        // Proxy confiable evalúa IP del cliente real 200.50.10.5
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimiter.permitirReserva(reqConfiable));
        }
        assertFalse(rateLimiter.permitirReserva(reqConfiable));

        // Proxy no confiable ignora la cabecera manipulada y evalúa remoteAddr 192.168.1.100
        assertTrue(rateLimiter.permitirReserva(reqNoConfiable), "IP real no confiable no debe verse afectada por el spoofing");
    }
}
