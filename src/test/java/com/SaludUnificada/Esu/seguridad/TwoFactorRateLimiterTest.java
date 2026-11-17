package com.SaludUnificada.Esu.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwoFactorRateLimiterTest {

    @Mock
    private RequestIpUtil requestIpUtil;

    private TwoFactorRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        when(requestIpUtil.obtenerIpCliente(any())).thenReturn("192.168.1.50");
        // Ventana de 1 minuto, máximo 3 intentos de challenge para testing
        rateLimiter = new TwoFactorRateLimiter(requestIpUtil, 60000L, 3, 5);
    }

    @Test
    @DisplayName("Challenge rate limit: permite hasta el máximo y bloquea en intentos subsiguientes")
    void permitirChallenge_bloqueaTrasExcederLimite() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        String user = "user@esu.gob.ar";

        assertTrue(rateLimiter.permitirChallenge(req, user), "Intento 1 permitido");
        assertTrue(rateLimiter.permitirChallenge(req, user), "Intento 2 permitido");
        assertTrue(rateLimiter.permitirChallenge(req, user), "Intento 3 permitido (límite)");
        assertFalse(rateLimiter.permitirChallenge(req, user), "Intento 4 debe ser bloqueado");
    }

    @Test
    @DisplayName("limpiarIntentosChallenge: resetea contador al autenticar exitosamente")
    void limpiarIntentosChallenge_reseteaContador() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        String user = "user@esu.gob.ar";

        rateLimiter.permitirChallenge(req, user);
        rateLimiter.permitirChallenge(req, user);
        rateLimiter.permitirChallenge(req, user);
        assertFalse(rateLimiter.permitirChallenge(req, user));

        rateLimiter.limpiarIntentosChallenge(req, user);

        assertTrue(rateLimiter.permitirChallenge(req, user), "Tras limpiar intentos, debe permitir nuevamente");
    }
}
