package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.auth.GoogleIdTokenPayload;
import com.SaludUnificada.Esu.dto.auth.GoogleLoginDtoRequest;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import com.SaludUnificada.Esu.servicio.AuditoriaSeguridadServicio;
import com.SaludUnificada.Esu.servicio.GoogleTokenVerifierService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleAuthControladorTest {

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
    private GoogleTokenVerifierService googleTokenVerifierService;

    @InjectMocks
    private AuthControlador authControlador;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authControlador.setGoogleTokenVerifierService(googleTokenVerifierService);
    }

    @Test
    @DisplayName("Usuario nuevo con Google: crea usuario con rol PACIENTE y retorna token")
    void loginGoogle_usuarioNuevo_creaPacienteYRetornaToken() {
        GoogleLoginDtoRequest request = new GoogleLoginDtoRequest("valid-token");
        GoogleIdTokenPayload payload = GoogleIdTokenPayload.builder()
                .sub("google-123456")
                .email("nuevo.paciente@gmail.com")
                .emailVerified(true)
                .givenName("Carlos")
                .familyName("Gomez")
                .build();

        when(googleTokenVerifierService.verificarToken("valid-token")).thenReturn(payload);
        when(usuarioRepositorio.findByGoogleSub("google-123456")).thenReturn(Optional.empty());
        when(usuarioRepositorio.findByEmail("nuevo.paciente@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");

        Usuario savedUser = new Usuario();
        savedUser.setId(99L);
        savedUser.setEmail("nuevo.paciente@gmail.com");
        savedUser.setRol("PACIENTE");
        savedUser.setGoogleSub("google-123456");
        savedUser.setEstadoActivo(true);
        savedUser.setEmailVerificado(true);
        savedUser.setPassword("encoded-password");

        when(usuarioRepositorio.save(any(Usuario.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generarToken(any(Authentication.class))).thenReturn("jwt.google.esu");

        var response = authControlador.loginConGoogle(request, null);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("PACIENTE", response.getBody().getRol());
        assertEquals("nuevo.paciente@gmail.com", response.getBody().getEmail());
        assertEquals("jwt.google.esu", response.getBody().getAccessToken());
        verify(usuarioRepositorio).save(argThat(u -> "PACIENTE".equals(u.getRol()) && "GOOGLE".equals(u.getAuthProvider())));
    }

    @Test
    @DisplayName("Usuario existente con Google: autentica y genera sesión")
    void loginGoogle_usuarioExistente_retornaToken() {
        GoogleLoginDtoRequest request = new GoogleLoginDtoRequest("existing-token");
        GoogleIdTokenPayload payload = GoogleIdTokenPayload.builder()
                .sub("google-already-registered")
                .email("existente@gmail.com")
                .emailVerified(true)
                .build();

        Usuario existingUser = new Usuario();
        existingUser.setId(101L);
        existingUser.setEmail("existente@gmail.com");
        existingUser.setRol("PACIENTE");
        existingUser.setGoogleSub("google-already-registered");
        existingUser.setEstadoActivo(true);
        existingUser.setPassword("pass");

        when(googleTokenVerifierService.verificarToken("existing-token")).thenReturn(payload);
        when(usuarioRepositorio.findByGoogleSub("google-already-registered")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.generarToken(any(Authentication.class))).thenReturn("jwt.token.existente");

        var response = authControlador.loginConGoogle(request, null);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(101L, response.getBody().getUsuarioId());
        assertEquals("jwt.token.existente", response.getBody().getAccessToken());
    }

    @Test
    @DisplayName("Email ya registrado localmente: rechaza unión automática para evitar secuestro de cuentas (409 CONFLICT)")
    void loginGoogle_emailLocalExistente_rechazaConConflict() {
        GoogleLoginDtoRequest request = new GoogleLoginDtoRequest("token-hijack-attempt");
        GoogleIdTokenPayload payload = GoogleIdTokenPayload.builder()
                .sub("google-rogue-sub")
                .email("cuenta.local@esu.gob.ar")
                .emailVerified(true)
                .build();

        Usuario localUser = new Usuario();
        localUser.setId(5L);
        localUser.setEmail("cuenta.local@esu.gob.ar");
        localUser.setAuthProvider("LOCAL");

        when(googleTokenVerifierService.verificarToken("token-hijack-attempt")).thenReturn(payload);
        when(usuarioRepositorio.findByGoogleSub("google-rogue-sub")).thenReturn(Optional.empty());
        when(usuarioRepositorio.findByEmail("cuenta.local@esu.gob.ar")).thenReturn(Optional.of(localUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authControlador.loginConGoogle(request, null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("registrado mediante cuenta local"));
    }

    @Test
    @DisplayName("Cuenta Google deshabilitada: retorna 403 FORBIDDEN")
    void loginGoogle_cuentaDeshabilitada_retornaForbidden() {
        GoogleLoginDtoRequest request = new GoogleLoginDtoRequest("token-disabled");
        GoogleIdTokenPayload payload = GoogleIdTokenPayload.builder()
                .sub("google-disabled")
                .email("disabled@gmail.com")
                .emailVerified(true)
                .build();

        Usuario disabledUser = new Usuario();
        disabledUser.setId(88L);
        disabledUser.setEmail("disabled@gmail.com");
        disabledUser.setGoogleSub("google-disabled");
        disabledUser.setEstadoActivo(false);

        when(googleTokenVerifierService.verificarToken("token-disabled")).thenReturn(payload);
        when(usuarioRepositorio.findByGoogleSub("google-disabled")).thenReturn(Optional.of(disabledUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authControlador.loginConGoogle(request, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Token inválido o expirado reportado por el servicio verificador: retorna 401 UNAUTHORIZED")
    void loginGoogle_tokenInvalido_retornaUnauthorized() {
        GoogleLoginDtoRequest request = new GoogleLoginDtoRequest("invalid-token");

        when(googleTokenVerifierService.verificarToken("invalid-token"))
                .thenThrow(new BadCredentialsException("El token de Google ha expirado."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authControlador.loginConGoogle(request, null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
