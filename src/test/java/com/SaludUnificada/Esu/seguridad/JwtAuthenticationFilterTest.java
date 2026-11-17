package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.servicio.RevokedTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private RevokedTokenService revokedTokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService, usuarioRepositorio);
        filter.setRevokedTokenService(revokedTokenService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Access token revocado por logout: el filtro rechaza el token y no autentica")
    void doFilter_tokenRevocadoPorLogout_noAutentica() throws Exception {
        String token = "valid.jwt.butRevoked";
        String jti = "revoked-jti-12345";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validarToken(token)).thenReturn(true);
        when(jwtTokenProvider.esTokenDeAcceso(token)).thenReturn(true);
        when(jwtTokenProvider.obtenerJtiDelJwt(token)).thenReturn(jti);
        when(revokedTokenService.estaRevocado(jti)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(customUserDetailsService);
    }

    @Test
    @DisplayName("Access token válido no revocado: autentica satisfactoriamente")
    void doFilter_tokenValido_autenticaContexto() throws Exception {
        String token = "valid.jwt.active";
        String jti = "active-jti-67890";
        String username = "paciente@esu.gob.ar";

        Usuario usuario = new Usuario();
        usuario.setEmail(username);
        usuario.setEstadoActivo(true);
        usuario.setTokenVersion(1);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validarToken(token)).thenReturn(true);
        when(jwtTokenProvider.esTokenDeAcceso(token)).thenReturn(true);
        when(jwtTokenProvider.obtenerJtiDelJwt(token)).thenReturn(jti);
        when(revokedTokenService.estaRevocado(jti)).thenReturn(false);
        when(jwtTokenProvider.obtenerUsernameDelJwt(token)).thenReturn(username);
        when(jwtTokenProvider.obtenerTokenVersionDelJwt(token)).thenReturn(1);
        when(usuarioRepositorio.findByEmail(username)).thenReturn(Optional.of(usuario));
        when(customUserDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Token version desactualizada (sesiones revocadas masivamente): no autentica")
    void doFilter_tokenVersionInvalida_noAutentica() throws Exception {
        String token = "outdated.version.jwt";
        String jti = "jti-outdated";
        String username = "usuario@esu.gob.ar";

        Usuario usuario = new Usuario();
        usuario.setEmail(username);
        usuario.setEstadoActivo(true);
        usuario.setTokenVersion(2); // Usuario incrementó su versión por reset/cambio de clave

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.validarToken(token)).thenReturn(true);
        when(jwtTokenProvider.esTokenDeAcceso(token)).thenReturn(true);
        when(jwtTokenProvider.obtenerJtiDelJwt(token)).thenReturn(jti);
        when(revokedTokenService.estaRevocado(jti)).thenReturn(false);
        when(jwtTokenProvider.obtenerUsernameDelJwt(token)).thenReturn(username);
        when(jwtTokenProvider.obtenerTokenVersionDelJwt(token)).thenReturn(1); // Token tiene versión 1
        when(usuarioRepositorio.findByEmail(username)).thenReturn(Optional.of(usuario));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(customUserDetailsService);
    }
}
