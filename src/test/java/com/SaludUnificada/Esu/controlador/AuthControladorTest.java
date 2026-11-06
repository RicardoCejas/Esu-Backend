package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.auth.CambiarPasswordDtoRequest;
import java.security.Principal;
import com.SaludUnificada.Esu.dto.auth.LoginDtoRequest;
import com.SaludUnificada.Esu.dto.auth.RegistroDtoRequest;
import com.SaludUnificada.Esu.dto.auth.ResetearPasswordDtoRequest;
import com.SaludUnificada.Esu.dto.auth.SolicitarRecuperacionDtoRequest;
import com.SaludUnificada.Esu.dto.auth.TwoFactorChallengeDtoRequest;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import com.SaludUnificada.Esu.seguridad.TotpServicio;
import com.SaludUnificada.Esu.servicio.AuditoriaSeguridadServicio;
import com.SaludUnificada.Esu.servicio.EmailNotificacionServicio;
import com.SaludUnificada.Esu.servicio.RecuperacionPasswordServicio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
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

    @Mock
    private TotpServicio totpServicio;

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
        when(jwtTokenProvider.generarTokenParaUsuario("medico@esu.com", "PROFESIONAL", 1))
                .thenReturn("mocked.jwt.token");

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
    void testLoginCon2FADevuelveDesafioSinAccessToken() {
        LoginDtoRequest request = new LoginDtoRequest("medico@esu.com", "password123");
        Authentication auth = mock(Authentication.class);
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setEmail("medico@esu.com");
        usuario.setRol("PROFESIONAL");
        usuario.setEmailVerificado(true);
        usuario.setEstadoActivo(true);
        usuario.setTwoFactorEnabled(true);

        when(auditoriaSeguridadServicio.estaBloqueadoPorIntentosFallidos("medico@esu.com")).thenReturn(false);
        when(usuarioRepositorio.findByEmail("medico@esu.com")).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtTokenProvider.generarTokenTemporal2FA("medico@esu.com")).thenReturn("temp.2fa.token");

        var response = authControlador.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(Boolean.TRUE.equals(response.getBody().getRequires2FA()));
        assertEquals("temp.2fa.token", response.getBody().getTempToken());
        assertNull(response.getBody().getAccessToken());
    }

    @Test
    void testChallenge2FAConTOTPValidoEmiteSesion() {
        TwoFactorChallengeDtoRequest request = new TwoFactorChallengeDtoRequest("temp.2fa.token", "123456", false);
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setEmail("medico@esu.com");
        usuario.setRol("PROFESIONAL");
        usuario.setEstadoActivo(true);
        usuario.setTwoFactorEnabled(true);
        usuario.setTwoFactorSecret("JBSWY3DPEHPK3PXP");

        when(jwtTokenProvider.validarToken("temp.2fa.token")).thenReturn(true);
        when(jwtTokenProvider.esToken2FA("temp.2fa.token")).thenReturn(true);
        when(jwtTokenProvider.obtenerUsernameDelJwt("temp.2fa.token")).thenReturn("medico@esu.com");
        when(usuarioRepositorio.findByEmail("medico@esu.com")).thenReturn(Optional.of(usuario));
        when(totpServicio.validarCodigo("JBSWY3DPEHPK3PXP", "123456", null)).thenReturn(123L);
        when(jwtTokenProvider.generarTokenParaUsuario("medico@esu.com", "PROFESIONAL", 1))
                .thenReturn("access.2fa.token");

        var response = authControlador.challenge2FA(request, new MockHttpServletResponse());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("access.2fa.token", response.getBody().getAccessToken());
        assertEquals(123L, usuario.getTwoFactorLastCounter());
    }

    @Test
    void testLoginEmailNoVerificadoAutoActiva() {
        LoginDtoRequest request = new LoginDtoRequest("noverificado@esu.com", "password123");
        Authentication auth = mock(Authentication.class);
        Usuario usuario = new Usuario();
        usuario.setId(30L);
        usuario.setEmail("noverificado@esu.com");
        usuario.setEmailVerificado(false);
        usuario.setEstadoActivo(true);
        usuario.setRol("PACIENTE");

        when(auditoriaSeguridadServicio.estaBloqueadoPorIntentosFallidos("noverificado@esu.com")).thenReturn(false);
        when(usuarioRepositorio.findByEmail("noverificado@esu.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtTokenProvider.generarTokenParaUsuario("noverificado@esu.com", "PACIENTE", 1))
                .thenReturn("mocked.jwt.token");

        var response = authControlador.login(request);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("mocked.jwt.token", response.getBody().getAccessToken());
        assertTrue(Boolean.TRUE.equals(response.getBody().getEmailVerificado()));
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
        usuarioGuardado.setPassword("hashed_password");

        Paciente pacienteGuardado = new Paciente();
        pacienteGuardado.setId(99L);
        pacienteGuardado.setNombre("Paciente");
        pacienteGuardado.setApellido("Registrado");
        pacienteGuardado.setDni("DNI-20");
        pacienteGuardado.setNroAfiliado("AF-DNI-20");
        pacienteGuardado.setUsuario(usuarioGuardado);

        when(usuarioRepositorio.existsByEmail("nuevo@esu.com")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("hashed_password");
        when(usuarioRepositorio.save(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(pacienteRepositorio.save(any(Paciente.class))).thenReturn(pacienteGuardado);
        when(jwtTokenProvider.generarToken(any(Authentication.class))).thenReturn("mocked.jwt.token");

        var response = authControlador.registro(request, null);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(20L, response.getBody().getUsuarioId());
        assertEquals(99L, response.getBody().getPacienteId());
        assertEquals("nuevo@esu.com", response.getBody().getEmail());
        assertEquals("mocked.jwt.token", response.getBody().getAccessToken());
        assertTrue(Boolean.TRUE.equals(response.getBody().getEmailVerificado()));
    }

    @Test
    void testRegistroEmailDuplicado() {
        RegistroDtoRequest request = new RegistroDtoRequest("existente@esu.com", "clave123", "PACIENTE");
        when(usuarioRepositorio.existsByEmail("existente@esu.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authControlador.registro(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void testRegistroDniDuplicado() {
        RegistroDtoRequest request = new RegistroDtoRequest();
        request.setEmail("nuevo@esu.com");
        request.setPassword("clave123");
        request.setDni("38123456");
        when(usuarioRepositorio.existsByEmail("nuevo@esu.com")).thenReturn(false);
        when(pacienteRepositorio.existsByDni("38123456")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authControlador.registro(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void testObtenerUsuarioActualConIdsDeDominio() {
        Principal principal = () -> "paciente@esu.com";
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("paciente@esu.com");
        usuario.setRol("PACIENTE");
        usuario.setEstadoActivo(true);
        usuario.setEmailVerificado(true);

        Paciente paciente = new Paciente();
        paciente.setId(9L);
        paciente.setNombre("Ana");
        paciente.setApellido("Perez");
        paciente.setDni("30111222");
        usuario.setPaciente(paciente);

        when(usuarioRepositorio.findByEmail("paciente@esu.com")).thenReturn(Optional.of(usuario));

        var response = authControlador.obtenerUsuarioActual(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().get("usuarioId"));
        assertEquals(9L, response.getBody().get("pacienteId"));
        assertNull(response.getBody().get("profesionalId"));
        assertEquals("Ana", response.getBody().get("nombre"));
        assertEquals("30111222", response.getBody().get("dni"));
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
        when(usuarioRepositorio.findByTokenVerificacion(anyString())).thenReturn(Optional.of(usuario));

        var res = authControlador.verificarEmail("token-valido-123");

        assertNotNull(res);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(usuario.getEmailVerificado());
        assertTrue(usuario.getTokenVerificacionUsado());
        assertNull(usuario.getTokenVerificacion());
        verify(usuarioRepositorio, times(1)).save(usuario);
    }

    @Test
    void testCambiarPasswordExitoso() {
        Principal principal = () -> "paciente@esu.com";
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setEmail("paciente@esu.com");
        usuario.setPassword("hashed-actual");
        usuario.setAuthProvider("LOCAL");

        when(usuarioRepositorio.findByEmail("paciente@esu.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("ClaveActual123!", "hashed-actual")).thenReturn(true);
        when(passwordEncoder.encode("NuevaClaveSuper456!")).thenReturn("hashed-nueva");

        CambiarPasswordDtoRequest req = new CambiarPasswordDtoRequest("ClaveActual123!", "NuevaClaveSuper456!");
        var res = authControlador.cambiarPassword(principal, req);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals("Contraseña actualizada exitosamente.", res.getBody().get("mensaje"));
        verify(usuarioRepositorio, times(1)).save(usuario);
    }

    @Test
    void testCambiarPasswordPasswordActualIncorrecta() {
        Principal principal = () -> "paciente@esu.com";
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setEmail("paciente@esu.com");
        usuario.setPassword("hashed-actual");
        usuario.setAuthProvider("LOCAL");

        when(usuarioRepositorio.findByEmail("paciente@esu.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Erronea123!", "hashed-actual")).thenReturn(false);

        CambiarPasswordDtoRequest req = new CambiarPasswordDtoRequest("Erronea123!", "NuevaClaveSuper456!");
        assertThrows(ResponseStatusException.class, () -> authControlador.cambiarPassword(principal, req));
    }
}