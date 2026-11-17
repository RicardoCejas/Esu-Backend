package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.TokenRevocado;
import com.SaludUnificada.Esu.repositorio.TokenRevocadoRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevokedTokenService {

    private final TokenRevocadoRepositorio tokenRevocadoRepositorio;

    @Transactional
    public void revocarToken(String jti, LocalDateTime fechaExpiracion, String razon) {
        if (jti == null || jti.isBlank()) return;
        String cleanJti = jti.trim();
        if (tokenRevocadoRepositorio.existsByJti(cleanJti)) {
            return; // Idempotente
        }
        LocalDateTime ahora = LocalDateTime.now();
        TokenRevocado revocado = TokenRevocado.builder()
                .jti(cleanJti)
                .fechaExpiracion(fechaExpiracion != null ? fechaExpiracion : ahora.plusDays(1))
                .fechaRevocacion(ahora)
                .razon(razon != null ? razon : "REVOCADO")
                .build();
        tokenRevocadoRepositorio.save(revocado);
        log.info("Token JWT con jti={} revocado satisfactoriamente. Razón: {}", cleanJti, razon);
    }

    @Transactional(readOnly = true)
    public boolean estaRevocado(String jti) {
        if (jti == null || jti.isBlank()) return false;
        return tokenRevocadoRepositorio.existsByJti(jti.trim());
    }

    @Transactional
    public int limpiarTokensExpirados() {
        int eliminados = tokenRevocadoRepositorio.limpiarTokensExpirados(LocalDateTime.now());
        if (eliminados > 0) {
            log.debug("Limpieza de tokens revocados expirados: {} registros depurados.", eliminados);
        }
        return eliminados;
    }
}
