package com.SaludUnificada.Esu.seguridad;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Value("${app.jwt.issuer:esu-backend}")
    private String jwtIssuer;

    @Value("${app.jwt.audience:esu-frontend}")
    private String jwtAudience;

    @PostConstruct
    public void validateSecret() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty() || jwtSecret.trim().length() < 32) {
            throw new IllegalStateException("JWT secret (app.jwt.secret) must be securely configured and at least 256 bits (32 characters). Application startup aborted.");
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generarToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date ahora = new Date();
        Date fechaExpiracion = new Date(ahora.getTime() + jwtExpirationMs);

        String roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .issuer(jwtIssuer)
                .audience().add(jwtAudience).and()
                .subject(userPrincipal.getUsername())
                .claim("roles", roles)
                .claim("tv", 1)
                .issuedAt(ahora)
                .expiration(fechaExpiracion)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generarTokenParaUsuario(String email, String rol, Integer tokenVersion) {
        Date ahora = new Date();
        Date fechaExpiracion = new Date(ahora.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .issuer(jwtIssuer)
                .audience().add(jwtAudience).and()
                .subject(email)
                .claim("roles", rol.startsWith("ROLE_") ? rol : "ROLE_" + rol)
                .claim("tv", tokenVersion != null ? tokenVersion : 1)
                .issuedAt(ahora)
                .expiration(fechaExpiracion)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generarTokenParaUsuario(String email, String rol) {
        return generarTokenParaUsuario(email, rol, 1);
    }

    public String obtenerUsernameDelJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public Integer obtenerTokenVersionDelJwt(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Object tv = claims.get("tv");
            if (tv instanceof Number) {
                return ((Number) tv).intValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parser()
                    .requireIssuer(jwtIssuer)
                    .requireAudience(jwtAudience)
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT inválido o expirado: {}", e.getMessage());
            return false;
        }
    }

    public Date obtenerFechaEmisionDelJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getIssuedAt();
    }
}
