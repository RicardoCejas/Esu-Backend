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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServicio {

    private final RefreshTokenRepositorio refreshTokenRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired(required = false)
    private RefreshTokenRevocationService refreshTokenRevocationService;

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

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

    public static String hashToken(String rawToken) {
        if (rawToken == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en la plataforma JVM.", e);
        }
    }

    @Transactional
    public RefreshToken crearRefreshToken(Usuario usuario) {
        String rawToken = generarTokenAleatorio();
        String tokenHash = hashToken(rawToken);
        LocalDateTime fechaCreacion = ahora();
        LocalDateTime fechaExpiracion = fechaCreacion.plusDays(refreshExpirationDays);

        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(tokenHash)
                .fechaCreacion(fechaCreacion)
                .fechaExpiracion(fechaExpiracion)
                .revocado(false)
                .build();

        RefreshToken guardado = refreshTokenRepositorio.save(refreshToken);
        guardado.setToken(rawToken);
        return guardado;
    }

    public record RefreshResult(JwtAuthDtoResponse authResponse, String newRefreshTokenStr) {}

    public RefreshResult rotarRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Refresh token no proporcionado o inválido.");
        }

        String tokenHash = hashToken(rawToken);
        LocalDateTime ahora = ahora();

        record StepOutcome(
                RefreshResult result,
                Long usuarioIdARevocarPorCompromiso,
                String mensajeRechazo
        ) {}

        Supplier<StepOutcome> dbStep = () -> {
            // Bloqueo pesimista para serializar rotaciones concurrentes de un mismo token y evitar condiciones de carrera
            RefreshToken tokenExistente = refreshTokenRepositorio.findByTokenHashForUpdate(tokenHash)
                    .orElseThrow(() -> {
                        log.warn("Intento de refresh con token inexistente.");
                        return new BadCredentialsException("Refresh token inválido.");
                    });

            Usuario usuario = tokenExistente.getUsuario();

            // Detección de reuso de token revocado (posible robo de sesión vs. carrera concurrente reciente)
            if (tokenExistente.isRevocado()) {
                String reemplazoHash = tokenExistente.getReemplazadoPor();
                if (reemplazoHash != null && !reemplazoHash.isBlank()) {
                    RefreshToken reemplazo = refreshTokenRepositorio.findByTokenHash(reemplazoHash).orElse(null);
                    if (reemplazo != null
                            && !reemplazo.isRevocado()
                            && reemplazo.getFechaCreacion() != null
                            && !reemplazo.getFechaCreacion().isBefore(ahora.minusSeconds(10))) {
                        log.warn("Colisión concurrente tolerada de refresh token para usuario ID={}. No se revocan otras sesiones.", usuario.getId());
                        return new StepOutcome(null, null, "Sesión actualizada concurrentemente. Reintente.");
                    }
                }

                // Commit/finaliza TX1 antes de invocar revocación REQUIRES_NEW para evitar autodeadlock InnoDB
                return new StepOutcome(null, usuario.getId(), "Sesión comprometida o revocada. Inicie sesión nuevamente.");
            }

            if (tokenExistente.estaExpirado(ahora)) {
                tokenExistente.setRevocado(true);
                refreshTokenRepositorio.save(tokenExistente);
                return new StepOutcome(null, null, "La sesión ha expirado. Inicie sesión nuevamente.");
            }

            if (!Boolean.TRUE.equals(usuario.getEstadoActivo())) {
                tokenExistente.setRevocado(true);
                refreshTokenRepositorio.save(tokenExistente);
                return new StepOutcome(null, null, "La cuenta de usuario se encuentra deshabilitada.");
            }

            // Rotación: revocar el token actual y generar uno nuevo
            String nuevoTokenRaw = generarTokenAleatorio();
            String nuevoTokenHash = hashToken(nuevoTokenRaw);

            tokenExistente.setRevocado(true);
            tokenExistente.setReemplazadoPor(nuevoTokenHash);
            refreshTokenRepositorio.save(tokenExistente);

            RefreshToken nuevoRefreshToken = RefreshToken.builder()
                    .usuario(usuario)
                    .tokenHash(nuevoTokenHash)
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

            return new StepOutcome(new RefreshResult(authResponse, nuevoTokenRaw), null, null);
        };

        StepOutcome outcome;
        if (transactionManager != null) {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            outcome = txTemplate.execute(status -> dbStep.get());
        } else {
            outcome = dbStep.get();
        }

        if (outcome == null) {
            throw new BadCredentialsException("No se pudo procesar la renovación de sesión.");
        }

        if (outcome.usuarioIdARevocarPorCompromiso() != null) {
            Long usuarioId = outcome.usuarioIdARevocarPorCompromiso();
            log.warn("Alerta de seguridad: Intento de reuso de refresh token ya revocado para usuario ID={}. Revocando todas las sesiones.", usuarioId);
            if (refreshTokenRevocationService != null) {
                refreshTokenRevocationService.revocarTodasLasSesionesPorCompromiso(usuarioId);
            } else {
                refreshTokenRepositorio.revocarTodosPorUsuarioId(usuarioId);
            }
        }

        if (outcome.mensajeRechazo() != null) {
            throw new BadCredentialsException(outcome.mensajeRechazo());
        }

        return outcome.result();
    }

    @Transactional
    public void revocarToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        String tokenHash = hashToken(rawToken);
        refreshTokenRepositorio.revocarPorTokenHash(tokenHash);
    }

    @Transactional
    public void revocarTodosLosTokensDeUsuario(Long usuarioId) {
        if (usuarioId != null) {
            refreshTokenRepositorio.revocarTodosPorUsuarioId(usuarioId);
        }
    }
}
