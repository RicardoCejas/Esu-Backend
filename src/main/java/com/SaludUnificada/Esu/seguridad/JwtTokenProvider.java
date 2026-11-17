package com.SaludUnificada.Esu.seguridad;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
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

    @Value("${app.jwt.two-factor-expiration-ms:300000}")
    private long twoFactorExpirationMs;

    @Autowired(required = false)
    private UsuarioRepositorio usuarioRepositorio;

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

        Integer tokenVersion = 1;
        if (usuarioRepositorio != null) {
            tokenVersion = usuarioRepositorio.findByEmail(userPrincipal.getUsername())
                    .map(Usuario::getTokenVersion)
                    .orElse(1);
        }
        if (tokenVersion == null) {
            tokenVersion = 1;
        }
        Long centroSaludId = null;
        String centroSaludNombre = null;
        if (usuarioRepositorio != null) {
            var uOpt = usuarioRepositorio.findByEmail(userPrincipal.getUsername());
            if (uOpt.isPresent()) {
                var u = uOpt.get();
                centroSaludId = u.getCentroSaludId();
                centroSaludNombre = u.getCentroSaludNombre();
            }
        }

        var builder = Jwts.builder()
                .issuer(jwtIssuer)
                .audience().add(jwtAudience).and()
                .id(UUID.randomUUID().toString())
                .subject(userPrincipal.getUsername())
                .claim("roles", roles)
                .claim("tv", tokenVersion)
                .claim("tipo", "access");
        if (centroSaludId != null) {
            builder.claim("centroSaludId", centroSaludId);
        }
        if (centroSaludNombre != null) {
            builder.claim("centroSaludNombre", centroSaludNombre);
        }
        return builder
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
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("roles", rol.startsWith("ROLE_") ? rol : "ROLE_" + rol)
                .claim("tv", tokenVersion != null ? tokenVersion : 1)
                .claim("tipo", "access")
                .issuedAt(ahora)
                .expiration(fechaExpiracion)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generarTokenParaUsuario(String email, String rol) {
        Integer tv = 1;
        if (usuarioRepositorio != null) {
            tv = usuarioRepositorio.findByEmail(email)
                    .map(Usuario::getTokenVersion)
                    .orElse(1);
        }
        return generarTokenParaUsuario(email, rol, tv != null ? tv : 1);
    }

    public String obtenerUsernameDelJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public String obtenerJtiDelJwt(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getId();
        } catch (Exception e) {
            return null;
        }
    }

    public Date obtenerFechaExpiracionDelJwt(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
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

    public String generarTokenTemporal2FA(String email, String jti) {
        Date ahora = new Date();
        Date fechaExpiracion = new Date(ahora.getTime() + twoFactorExpirationMs);

        return Jwts.builder()
                .issuer(jwtIssuer)
                .audience().add(jwtAudience).and()
                .id(jti != null ? jti : UUID.randomUUID().toString())
                .subject(email)
                .claim("tipo", "2fa")
                .issuedAt(ahora)
                .expiration(fechaExpiracion)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generarTokenTemporal2FA(String email) {
        return generarTokenTemporal2FA(email, UUID.randomUUID().toString());
    }

    public String obtenerTipoDelJwt(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("tipo", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean esTokenDeAcceso(String token) {
        String tipo = obtenerTipoDelJwt(token);
        return tipo == null || "access".equals(tipo);
    }

    public Long obtenerCentroSaludIdDelJwt(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object cid = claims.get("centroSaludId");
            if (cid instanceof Number) {
                return ((Number) cid).longValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String obtenerCentroSaludNombreDelJwt(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("centroSaludNombre", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean esToken2FA(String token) {
        return "2fa".equals(obtenerTipoDelJwt(token));
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
