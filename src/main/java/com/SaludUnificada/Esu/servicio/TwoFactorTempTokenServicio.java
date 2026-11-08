package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.TwoFactorTempToken;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.TwoFactorTempTokenRepositorio;
import com.SaludUnificada.Esu.seguridad.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorTempTokenServicio {

    private final TwoFactorTempTokenRepositorio tempTokenRepositorio;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public String generarYRegistrarTokenTemporal(Usuario usuario) {
        String jti = UUID.randomUUID().toString();
        LocalDateTime ahora = LocalDateTime.now();
        // 5 minutos de validez para el token temporal 2FA
        LocalDateTime expiracion = ahora.plusMinutes(5);

        TwoFactorTempToken entidad = TwoFactorTempToken.builder()
                .usuario(usuario)
                .jti(jti)
                .consumido(false)
                .fechaCreacion(ahora)
                .fechaExpiracion(expiracion)
                .build();
        tempTokenRepositorio.save(entidad);

        return jwtTokenProvider.generarTokenTemporal2FA(usuario.getEmail(), jti);
    }

    @Transactional(readOnly = true)
    public boolean estaDisponible(String jti) {
        if (jti == null || jti.isBlank()) return false;
        Optional<TwoFactorTempToken> opt = tempTokenRepositorio.findByJti(jti.trim());
        if (opt.isEmpty()) return false;
        TwoFactorTempToken token = opt.get();
        return !token.isConsumido() && !token.estaExpirado(LocalDateTime.now());
    }

    @Transactional
    public boolean consumirToken(String jti) {
        if (jti == null || jti.isBlank()) return false;
        int filasAfectadas = tempTokenRepositorio.consumirTokenAtomicamente(jti.trim(), LocalDateTime.now());
        if (filasAfectadas > 0) {
            log.info("Token temporal 2FA consumido atómicamente con jti={}", jti);
            return true;
        }
        log.warn("Rechazo de reutilización/expiración para token temporal 2FA con jti={}", jti);
        return false;
    }
}
