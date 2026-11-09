package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.auth.JwtAuthDtoResponse;
import com.SaludUnificada.Esu.entidad.RefreshToken;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.RefreshTokenRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServicioTest {

    @Mock
    private RefreshTokenRepositorio refreshTokenRepositorio;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRevocationService refreshTokenRevocationService;

    @InjectMocks
    private RefreshTokenServicio refreshTokenServicio;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setEmail("test@paciente.com");
        usuario.setRol("PACIENTE");
        usuario.setEstadoActivo(true);
        usuario.setTokenVersion(1);

        ReflectionTestUtils.setField(refreshTokenServicio, "refreshExpirationDays", 7L);
        ReflectionTestUtils.setField(refreshTokenServicio, "refreshTokenRevocationService", refreshTokenRevocationService);
    }

    @Test
    @DisplayName("Crear refresh token: calcula hash SHA-256 y persiste entidad")
    void crearRefreshToken_calculaHashYPersiste() {
        when(refreshTokenRepositorio.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken rt = refreshTokenServicio.crearRefreshToken(usuario);

        assertNotNull(rt);
        assertNotNull(rt.getToken());
        assertNotNull(rt.getTokenHash());
        assertEquals(64, rt.getTokenHash().length());
        assertEquals(usuario, rt.getUsuario());
        assertFalse(rt.isRevocado());
        verify(refreshTokenRepositorio).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Rotar refresh token válido: revoca token anterior, persiste nuevo token con hash y retorna nuevo JWT")
    void rotarRefreshToken_valido_rotaSatisfactoriamente() {
        String rawToken = "raw-token-1234567890";
        String tokenHash = RefreshTokenServicio.hashToken(rawToken);

        RefreshToken tokenExistente = RefreshToken.builder()
                .id(1L)
                .usuario(usuario)
                .tokenHash(tokenHash)
                .fechaCreacion(LocalDateTime.now().minusDays(1))
                .fechaExpiracion(LocalDateTime.now().plusDays(6))
                .revocado(false)
                .build();

        when(refreshTokenRepositorio.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(tokenExistente));
        when(jwtTokenProvider.generarTokenParaUsuario(eq(usuario.getEmail()), eq(usuario.getRol()), eq(1)))
                .thenReturn("new.access.token");
        when(refreshTokenRepositorio.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenServicio.RefreshResult result = refreshTokenServicio.rotarRefreshToken(rawToken);

        assertNotNull(result);
        assertEquals("new.access.token", result.authResponse().getAccessToken());
        assertNotNull(result.newRefreshTokenStr());
        assertNotEquals(rawToken, result.newRefreshTokenStr());
        assertTrue(tokenExistente.isRevocado());
        assertNotNull(tokenExistente.getReemplazadoPor());
    }

    @Test
    @DisplayName("Colisión concurrente reciente: tolera reintento sin revocar todas las sesiones del usuario")
    void rotarRefreshToken_colisionConcurrenteReciente_noRevocaOtrasSesiones() {
        String rawToken = "concurrent-race-token";
        String tokenHash = RefreshTokenServicio.hashToken(rawToken);
        String reemplazoHash = RefreshTokenServicio.hashToken("newly-issued-replacement-token");
        LocalDateTime ahoraArg = LocalDateTime.now(ZoneId.of("America/Argentina/Cordoba"));

        RefreshToken tokenExistente = RefreshToken.builder()
                .id(4L)
                .usuario(usuario)
                .tokenHash(tokenHash)
                .reemplazadoPor(reemplazoHash)
                .fechaCreacion(ahoraArg.minusHours(1))
                .fechaExpiracion(ahoraArg.plusDays(6))
                .revocado(true)
                .build();

        RefreshToken tokenReemplazo = RefreshToken.builder()
                .id(5L)
                .usuario(usuario)
                .tokenHash(reemplazoHash)
                .fechaCreacion(ahoraArg.minusSeconds(2))
                .fechaExpiracion(ahoraArg.plusDays(7))
                .revocado(false)
                .build();

        when(refreshTokenRepositorio.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(tokenExistente));
        when(refreshTokenRepositorio.findByTokenHash(reemplazoHash)).thenReturn(Optional.of(tokenReemplazo));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> refreshTokenServicio.rotarRefreshToken(rawToken));

        assertTrue(ex.getMessage().contains("concurrentemente"));
        verifyNoInteractions(refreshTokenRevocationService);
        verify(refreshTokenRepositorio, never()).revocarTodosPorUsuarioId(any());
    }

    @Test
    @DisplayName("Detección de reuso de token revocado: revoca todas las sesiones en transacción independiente")
    void rotarRefreshToken_tokenYaRevocado_disparaRevocacionTotal() {
        String rawToken = "reused-revoked-token";
        String tokenHash = RefreshTokenServicio.hashToken(rawToken);

        RefreshToken tokenRevocado = RefreshToken.builder()
                .id(2L)
                .usuario(usuario)
                .tokenHash(tokenHash)
                .fechaCreacion(LocalDateTime.now().minusDays(2))
                .fechaExpiracion(LocalDateTime.now().plusDays(5))
                .revocado(true)
                .build();

        when(refreshTokenRepositorio.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(tokenRevocado));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> refreshTokenServicio.rotarRefreshToken(rawToken));

        assertTrue(ex.getMessage().contains("Sesión comprometida"));
        verify(refreshTokenRevocationService).revocarTodasLasSesionesPorCompromiso(usuario.getId());
    }

    @Test
    @DisplayName("Token expirado: marca revocado y rechaza con BadCredentialsException")
    void rotarRefreshToken_expirado_rechaza() {
        String rawToken = "expired-token";
        String tokenHash = RefreshTokenServicio.hashToken(rawToken);

        RefreshToken tokenExpirado = RefreshToken.builder()
                .id(3L)
                .usuario(usuario)
                .tokenHash(tokenHash)
                .fechaCreacion(LocalDateTime.now().minusDays(10))
                .fechaExpiracion(LocalDateTime.now().minusDays(3))
                .revocado(false)
                .build();

        when(refreshTokenRepositorio.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(tokenExpirado));

        assertThrows(BadCredentialsException.class,
                () -> refreshTokenServicio.rotarRefreshToken(rawToken));

        assertTrue(tokenExpirado.isRevocado());
        verify(refreshTokenRepositorio).save(tokenExpirado);
    }
}
