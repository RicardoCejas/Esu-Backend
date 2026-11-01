package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.auth.JwtAuthDtoResponse;
import com.SaludUnificada.Esu.entidad.RefreshToken;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.RefreshTokenRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServicio {

    private final RefreshTokenRepositorio refreshTokenRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired(required = false)
    private Clock clock;

    @Autowired(required = false)
    private ZoneId turnosZoneId;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private LocalDateTime ahora() {
        ZoneId zone = turnosZoneId != null ? turnosZoneId : ZoneId.of("America/Argentina/Cordoba");
        Clock c = clock != null ? clock.withZone(zone) : Clock.system(zone);
        return LocalDateTime.now(c);
    }

    private String generarTokenAleatorio() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Transactional
    public RefreshToken crearRefreshToken(Usuario usuario) {
        String tokenStr = generarTokenAleatorio();
        LocalDateTime fechaCreacion = ahora();
        LocalDateTime fechaExpiracion = fechaCreacion.plusDays(refreshExpirationDays);

        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(tokenStr)
                .fechaCreacion(fechaCreacion)
                .fechaExpiracion(fechaExpiracion)
                .revocado(false)
                .build();

        return refreshTokenRepositorio.save(refreshToken);
    }

    public record RefreshResult(JwtAuthDtoResponse authResponse, String newRefreshTokenStr) {}

    @Transactional
    public RefreshResult rotarRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Refresh token no proporcionado o inválido.");
        }

        RefreshToken tokenExistente = refreshTokenRepositorio.findByToken(rawToken.trim())
                .orElseThrow(() -> {
                    log.warn("Intento de refresh con token inexistente.");
                    return new BadCredentialsException("Refresh token inválido.");
                });

        Usuario usuario = tokenExistente.getUsuario();
        LocalDateTime ahora = ahora();

        // Detección de reuso de token revocado (posible robo de sesión)
        if (tokenExistente.isRevocado()) {
            log.warn("Alerta de seguridad: Intento de reuso de refresh token ya revocado para usuario ID={}. Revocando todas las sesiones.", usuario.getId());
            refreshTokenRepositorio.revocarTodosPorUsuarioId(usuario.getId());
            throw new BadCredentialsException("Sesión comprometida o revocada. Inicie sesión nuevamente.");
        }

        if (tokenExistente.estaExpirado(ahora)) {
            tokenExistente.setRevocado(true);
            refreshTokenRepositorio.save(tokenExistente);
            throw new BadCredentialsException("La sesión ha expirado. Inicie sesión nuevamente.");
        }

        if (!Boolean.TRUE.equals(usuario.getEstadoActivo())) {
            tokenExistente.setRevocado(true);
            refreshTokenRepositorio.save(tokenExistente);
            throw new BadCredentialsException("La cuenta de usuario se encuentra deshabilitada.");
        }

        // Rotación: revocar el token actual y generar uno nuevo
        String nuevoTokenStr = generarTokenAleatorio();
        tokenExistente.setRevocado(true);
        tokenExistente.setReemplazadoPor(nuevoTokenStr);
        refreshTokenRepositorio.save(tokenExistente);

        RefreshToken nuevoRefreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(nuevoTokenStr)
                .fechaCreacion(ahora)
                .fechaExpiracion(ahora.plusDays(refreshExpirationDays))
                .revocado(false)
                .build();
        refreshTokenRepositorio.save(nuevoRefreshToken);

        // Generar nuevo JWT Access Token
        String nuevoAccessToken = jwtTokenProvider.generarTokenParaUsuario(
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getTokenVersion()
        );

        JwtAuthDtoResponse authResponse = JwtAuthDtoResponse.builder()
                .accessToken(nuevoAccessToken)
                .tokenType("Bearer")
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .build();

        return new RefreshResult(authResponse, nuevoTokenStr);
    }

    @Transactional
    public void revocarToken(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            refreshTokenRepositorio.revocarPorToken(rawToken.trim());
        }
    }

    @Transactional
    public void revocarTodosDeUsuario(Long usuarioId) {
        if (usuarioId != null) {
            refreshTokenRepositorio.revocarTodosPorUsuarioId(usuarioId);
        }
    }
}
