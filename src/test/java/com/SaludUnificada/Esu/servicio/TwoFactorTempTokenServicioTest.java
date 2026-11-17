package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.TwoFactorTempToken;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.TwoFactorTempTokenRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorTempTokenServicioTest {

    @Mock
    private TwoFactorTempTokenRepositorio tempTokenRepositorio;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private TwoFactorTempTokenServicio tempTokenServicio;

    @Test
    @DisplayName("Generar token temporal: persiste entidad y delega generación JWT con jti")
    void generarYRegistrarTokenTemporal_persisteYGeneraJwt() {
        Usuario usuario = new Usuario();
        usuario.setId(15L);
        usuario.setEmail("usuario2fa@esu.gob.ar");

        when(jwtTokenProvider.generarTokenTemporal2FA(eq("usuario2fa@esu.gob.ar"), any(String.class)))
                .thenReturn("signed.2fa.jwt");

        String token = tempTokenServicio.generarYRegistrarTokenTemporal(usuario);

        assertEquals("signed.2fa.jwt", token);
        verify(tempTokenRepositorio).save(any(TwoFactorTempToken.class));
    }

    @Test
    @DisplayName("estaDisponible: retorna true solo si no consumido y no expirado")
    void estaDisponible_evaluaCorrectamente() {
        TwoFactorTempToken disponible = TwoFactorTempToken.builder()
                .jti("jti-disp")
                .consumido(false)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(3))
                .build();

        TwoFactorTempToken consumido = TwoFactorTempToken.builder()
                .jti("jti-cons")
                .consumido(true)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(3))
                .build();

        when(tempTokenRepositorio.findByJti("jti-disp")).thenReturn(Optional.of(disponible));
        when(tempTokenRepositorio.findByJti("jti-cons")).thenReturn(Optional.of(consumido));
        when(tempTokenRepositorio.findByJti("inexistente")).thenReturn(Optional.empty());

        assertTrue(tempTokenServicio.estaDisponible("jti-disp"));
        assertFalse(tempTokenServicio.estaDisponible("jti-cons"));
        assertFalse(tempTokenServicio.estaDisponible("inexistente"));
    }

    @Test
    @DisplayName("consumirToken: actualización atómica retorna true en primer uso, false en reuso")
    void consumirToken_atomico() {
        when(tempTokenRepositorio.consumirTokenAtomicamente(eq("jti-valido"), any(LocalDateTime.class))).thenReturn(1);
        when(tempTokenRepositorio.consumirTokenAtomicamente(eq("jti-reusado"), any(LocalDateTime.class))).thenReturn(0);

        assertTrue(tempTokenServicio.consumirToken("jti-valido"));
        assertFalse(tempTokenServicio.consumirToken("jti-reusado"));
    }
}
