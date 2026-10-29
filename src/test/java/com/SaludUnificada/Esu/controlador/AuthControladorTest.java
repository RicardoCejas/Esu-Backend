package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.auth.LoginDtoRequest;
import com.SaludUnificada.Esu.dto.auth.RegistroDtoRequest;
import com.SaludUnificada.Esu.dto.auth.ResetearPasswordDtoRequest;
import com.SaludUnificada.Esu.dto.auth.SolicitarRecuperacionDtoRequest;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import com.SaludUnificada.Esu.servicio.AuditoriaSeguridadServicio;
import com.SaludUnificada.Esu.servicio.EmailNotificacionServicio;
import com.SaludUnificada.Esu.servicio.RecuperacionPasswordServicio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControladorTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuditoriaSeguridadServicio auditoriaSeguridadServicio;

    @Mock
    private RecuperacionPasswordServicio recuperacionPasswordServicio;

    @Mock
    private EmailNotificacionServicio emailNotificacionServicio;

    @InjectMocks
    private AuthControlador authControlador;

    @Test
    void testLoginExitoso() {
        LoginDtoRequest request = new LoginDtoRequest("medico@esu.com", "password123");
        Authentication auth = mock(Authentication.class);
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setEmail("medico@esu.com");
        usuario.setRol("PROFESIONAL");
        usuario.setEmailVerificado(true);
        usuario.setEstadoActivo(true);

        when(auditoriaSeguridadServicio.estaBloqueadoPorIntentosFallidos("medico@esu.com")).thenReturn(false);
        when(usuarioRepositorio.findByEmail("medico@esu.com")).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtTokenProvider.generarToken(auth)).thenReturn("mocked.jwt.token");

        var response = authControlador.login(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("mocked.jwt.token", response.getBody().getAccessToken());
        assertEquals("Bearer", response.getBody().getTokenType());
        assertEquals("PROFESIONAL", response.getBody().getRol());
        assertEquals(10L, response.getBody().getUsuarioId());
    }

    @Test
    void testLoginEmailNoVerificado() {
        LoginDtoRequest request = new LoginDtoRequest("noverificado@esu.com", "password123");
        Usuario usuario = new Usuario();
        usuario.setEmail("noverificado@esu.com");
        usuario.setEmailVerificado(false);
        usuario.setEstadoActivo(true);

        when(usuarioRepositorio.findByEmail("noverificado@esu.com")).thenReturn(Optional.of(usuario));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authControlador.login(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("confirmar su dirección de correo"));
    }

    @Test
    void testLoginBloqueadoPorIntentosFallidos() {
        LoginDtoRequest request = new LoginDtoRequest("bloqueado@esu.com", "password123");
        when(auditoriaSeguridadServicio.estaBloqueadoPorIntentosFallidos("bloqueado@esu.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authControlador.login(request));
        assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
    }

    @Test
    void testRegistroExitoso() {
        RegistroDtoRequest request = new RegistroDtoRequest("nuevo@esu.com", "clave123", "PACIENTE");
        Usuario usuarioGuardado = new Usuario();
        usuarioGuardado.setId(20L);
        usuarioGuardado.setEmail("nuevo@esu.com");
        usuarioGuardado.setRol("PACIENTE");

        when(usuarioRepositorio.existsByEmail("nuevo@esu.com")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("hashed_password");
        when(usuarioRepositorio.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        var response = authControlador.registro(request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(20L, response.getBody().getUsuarioId());
        assertEquals("nuevo@esu.com", response.getBody().getEmail());
        assertNull(response.getBody().getAccessToken());
    }

    @Test
    void testRegistroEmailDuplicado() {
        RegistroDtoRequest request = new RegistroDtoRequest("existente@esu.com", "clave123", "PACIENTE");
        when(usuarioRepositorio.existsByEmail("existente@esu.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authControlador.registro(request));
    }

    @Test
    void testSolicitarRecuperacion() {
        when(auditoriaSeguridadServicio.puedeSolicitarRecuperacion(any())).thenReturn(true);
        SolicitarRecuperacionDtoRequest req = new SolicitarRecuperacionDtoRequest("user@esu.com");
        var res = authControlador.solicitarRecuperacion(req);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        verify(recuperacionPasswordServicio, times(1)).solicitarRecuperacion(req);
    }

    @Test
    void testResetearPassword() {
        ResetearPasswordDtoRequest req = new ResetearPasswordDtoRequest("tok-123", "NuevaClave!");
        var res = authControlador.resetearPassword(req);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        verify(recuperacionPasswordServicio, times(1)).resetearPassword(req);
    }

    @Test
    void testReenviarVerificacionEmail() {
        when(auditoriaSeguridadServicio.puedeReenviarVerificacion(any())).thenReturn(true);
        Usuario usuario = new Usuario();
        usuario.setEmail("test@paciente.com");
        usuario.setEmailVerificado(false);
        when(usuarioRepositorio.findByEmail("test@paciente.com")).thenReturn(Optional.of(usuario));

        var res = authControlador.reenviarVerificacionEmail(java.util.Map.of("email", "test@paciente.com"));

        assertNotNull(res);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(emailNotificacionServicio, times(1)).enviarEmailVerificacion(eq("test@paciente.com"), eq("test"), any(String.class));
    }

    @Test
    void testVerificarEmailExitoso() {
        Usuario usuario = new Usuario();
        usuario.setTokenVerificacion("token-valido-123");
        usuario.setEmailVerificado(false);
        usuario.setFechaExpiracionVerificacion(LocalDateTime.now().plusHours(12));
        usuario.setTokenVerificacionUsado(false);
        when(usuarioRepositorio.findByTokenVerificacion("token-valido-123")).thenReturn(Optional.of(usuario));

        var res = authControlador.verificarEmail("token-valido-123");

        assertNotNull(res);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(usuario.getEmailVerificado());
        assertTrue(usuario.getTokenVerificacionUsado());
        assertNull(usuario.getTokenVerificacion());
        verify(usuarioRepositorio, times(1)).save(usuario);
    }
}
