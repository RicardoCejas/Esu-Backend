package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.auth.LoginDtoRequest;
import com.SaludUnificada.Esu.dto.auth.RegistroDtoRequest;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControladorTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

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

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtTokenProvider.generarToken(auth)).thenReturn("mocked.jwt.token");
        when(usuarioRepositorio.findByEmail("medico@esu.com")).thenReturn(Optional.of(usuario));

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
    void testRegistroExitoso() {
        RegistroDtoRequest request = new RegistroDtoRequest("nuevo@esu.com", "clave123", "PACIENTE");
        Usuario usuarioGuardado = new Usuario();
        usuarioGuardado.setId(20L);
        usuarioGuardado.setEmail("nuevo@esu.com");
        usuarioGuardado.setRol("PACIENTE");

        when(usuarioRepositorio.existsByEmail("nuevo@esu.com")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("hashed_password");
        when(usuarioRepositorio.save(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(jwtTokenProvider.generarTokenParaUsuario("nuevo@esu.com", "PACIENTE")).thenReturn("mocked.jwt.token");

        var response = authControlador.registro(request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(20L, response.getBody().getUsuarioId());
        assertEquals("nuevo@esu.com", response.getBody().getEmail());
    }

    @Test
    void testRegistroEmailDuplicado() {
        RegistroDtoRequest request = new RegistroDtoRequest("existente@esu.com", "clave123", "PACIENTE");
        when(usuarioRepositorio.existsByEmail("existente@esu.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authControlador.registro(request));
    }
}
