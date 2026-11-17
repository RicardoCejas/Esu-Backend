package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.TokenRevocado;
import com.SaludUnificada.Esu.repositorio.TokenRevocadoRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevokedTokenServiceTest {

    @Mock
    private TokenRevocadoRepositorio tokenRevocadoRepositorio;

    @InjectMocks
    private RevokedTokenService revokedTokenService;

    @Test
    @DisplayName("Revocar token: persiste en repositorio si no existía")
    void revocarToken_persisteNuevo() {
        String jti = "jti-logout-1";
        LocalDateTime expiracion = LocalDateTime.now().plusHours(2);

        when(tokenRevocadoRepositorio.existsByJti(jti)).thenReturn(false);

        revokedTokenService.revocarToken(jti, expiracion, "LOGOUT");

        verify(tokenRevocadoRepositorio).save(any(TokenRevocado.class));
    }

    @Test
    @DisplayName("Revocar token: es idempotente si ya fue revocado previamente")
    void revocarToken_idempotente() {
        String jti = "jti-already-revoked";

        when(tokenRevocadoRepositorio.existsByJti(jti)).thenReturn(true);

        revokedTokenService.revocarToken(jti, LocalDateTime.now().plusHours(1), "LOGOUT");

        verify(tokenRevocadoRepositorio, never()).save(any(TokenRevocado.class));
    }

    @Test
    @DisplayName("estaRevocado: consulta existsByJti")
    void estaRevocado_consultaRepositorio() {
        when(tokenRevocadoRepositorio.existsByJti("jti-123")).thenReturn(true);
        when(tokenRevocadoRepositorio.existsByJti("jti-456")).thenReturn(false);

        assertTrue(revokedTokenService.estaRevocado("jti-123"));
        assertFalse(revokedTokenService.estaRevocado("jti-456"));
        assertFalse(revokedTokenService.estaRevocado(null));
        assertFalse(revokedTokenService.estaRevocado("   "));
    }

    @Test
    @DisplayName("limpiarTokensExpirados: invoca query de depuración")
    void limpiarTokensExpirados_invocaRepo() {
        when(tokenRevocadoRepositorio.limpiarTokensExpirados(any(LocalDateTime.class))).thenReturn(5);

        int eliminados = revokedTokenService.limpiarTokensExpirados();
        assertEquals(5, eliminados);
    }
}
