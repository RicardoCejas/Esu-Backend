package com.SaludUnificada.Esu.seguridad;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final int MIN_SECRET_BYTES = 32; // 256 bits para HMAC-SHA256

    // Issue #117: sin valor por defecto. Debe provenir de la variable de entorno JWT_SECRET.
    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}") // 24 horas por defecto
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}") // 7 dias
    private long refreshExpirationMs;

    @Value("${app.jwt.two-factor-expiration-ms:300000}") // 5 minutos
    private long twoFactorExpirationMs;

    @PostConstruct
    void validarConfiguracion() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "La propiedad 'app.jwt.secret' es obligatoria. Configure la variable de entorno JWT_SECRET.");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "La propiedad 'app.jwt.secret' debe tener al menos " + MIN_SECRET_BYTES + " bytes (256 bits).");
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generarToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        String roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return construirToken(userPrincipal.getUsername(), roles, "access", jwtExpirationMs);
    }

    public String generarTokenParaUsuario(String email, String rol) {
        String authority = rol.startsWith("ROLE_") ? rol : "ROLE_" + rol;
        return construirToken(email, authority, "access", jwtExpirationMs);
    }

    public String generarRefreshToken(String email) {
        return construirToken(email, null, "refresh", refreshExpirationMs);
    }

    public String generarTokenTemporal2FA(String email) {
        return construirToken(email, null, "2fa", twoFactorExpirationMs);
    }

    private String construirToken(String subject, String roles, String tipo, long expiracionMs) {
        Date ahora = new Date();
        Date fechaExpiracion = new Date(ahora.getTime() + expiracionMs);

        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .claim("tipo", tipo)
                .issuedAt(ahora)
                .expiration(fechaExpiracion)
                .signWith(getSigningKey(), Jwts.SIG.HS256);

        if (roles != null && !roles.isBlank()) {
            builder.claim("roles", roles);
        }

        return builder.compact();
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String obtenerUsernameDelJwt(String token) {
        return parsearClaims(token).getSubject();
    }

    /** Issue #117: identificador único del token, usado para revocación (logout). */
    public String obtenerJti(String token) {
        return parsearClaims(token).getId();
    }

    /** Issue #117: fecha de expiración del token, usada para acotar la revocación. */
    public Date obtenerExpiracion(String token) {
        return parsearClaims(token).getExpiration();
    }

    public String obtenerTipo(String token) {
        return parsearClaims(token).get("tipo", String.class);
    }

    public boolean esTokenDeAcceso(String token) {
        String tipo = obtenerTipo(token);
        return tipo == null || "access".equals(tipo);
    }

    public boolean esTokenRefresh(String token) {
        return "refresh".equals(obtenerTipo(token));
    }

    public boolean esToken2FA(String token) {
        return "2fa".equals(obtenerTipo(token));
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
