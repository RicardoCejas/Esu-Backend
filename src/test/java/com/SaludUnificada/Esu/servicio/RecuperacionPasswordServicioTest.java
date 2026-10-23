package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.auth.ResetearPasswordDtoRequest;
import com.SaludUnificada.Esu.dto.auth.SolicitarRecuperacionDtoRequest;
import com.SaludUnificada.Esu.entidad.TokenRecuperacion;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.TokenRecuperacionRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecuperacionPasswordServicioTest {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private TokenRecuperacionRepositorio tokenRepositorio;

    @Mock
    private EmailNotificacionServicio emailServicio;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RecuperacionPasswordServicio recuperacionServicio;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setEmail("test@paciente.com");
        usuario.setPassword("hashViejo");
        usuario.setRol("PACIENTE");
    }

    @Test
    void solicitarRecuperacion_usuarioExistente_guardaTokenYEnviaEmail() {
        SolicitarRecuperacionDtoRequest request = new SolicitarRecuperacionDtoRequest("test@paciente.com");
        when(usuarioRepositorio.findByEmail("test@paciente.com")).thenReturn(Optional.of(usuario));

        recuperacionServicio.solicitarRecuperacion(request);

        verify(tokenRepositorio, times(1)).save(any(TokenRecuperacion.class));
        verify(emailServicio, times(1)).enviarTokenRecuperacion(eq("test@paciente.com"), anyString(), anyString());
    }

    @Test
    void resetearPassword_tokenValido_actualizaPasswordEInvalidaToken() {
        TokenRecuperacion token = new TokenRecuperacion();
        token.setId(1L);
        token.setToken("uuid-1234");
        token.setUsuario(usuario);
        token.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
        token.setUsado(false);

        when(tokenRepositorio.findByToken("uuid-1234")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NuevaClave123!")).thenReturn("hashNuevo");

        ResetearPasswordDtoRequest request = new ResetearPasswordDtoRequest("uuid-1234", "NuevaClave123!");
        recuperacionServicio.resetearPassword(request);

        assertEquals("hashNuevo", usuario.getPassword());
        assertTrue(token.getUsado());
        verify(usuarioRepositorio, times(1)).save(usuario);
        verify(tokenRepositorio, times(1)).save(token);
    }

    @Test
    void resetearPassword_tokenYaUsado_lanzaExcepcion() {
        TokenRecuperacion token = new TokenRecuperacion();
        token.setToken("uuid-usado");
        token.setUsuario(usuario);
        token.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
        token.setUsado(true);

        when(tokenRepositorio.findByToken("uuid-usado")).thenReturn(Optional.of(token));

        ResetearPasswordDtoRequest request = new ResetearPasswordDtoRequest("uuid-usado", "NuevaClave123!");
        assertThrows(IllegalStateException.class, () -> recuperacionServicio.resetearPassword(request));
    }

    @Test
    void resetearPassword_tokenExpirado_lanzaExcepcion() {
        TokenRecuperacion token = new TokenRecuperacion();
        token.setToken("uuid-vencido");
        token.setUsuario(usuario);
        token.setFechaExpiracion(LocalDateTime.now().minusMinutes(5));
        token.setUsado(false);

        when(tokenRepositorio.findByToken("uuid-vencido")).thenReturn(Optional.of(token));

        ResetearPasswordDtoRequest request = new ResetearPasswordDtoRequest("uuid-vencido", "NuevaClave123!");
        assertThrows(IllegalStateException.class, () -> recuperacionServicio.resetearPassword(request));
    }
}
