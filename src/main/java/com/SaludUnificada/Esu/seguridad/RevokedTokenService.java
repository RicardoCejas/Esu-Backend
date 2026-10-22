package com.SaludUnificada.Esu.seguridad;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issue #117: registro de tokens revocados (logout) por su identificador {@code jti}.
 *
 * <p>Almacenamiento en memoria: suficiente para un despliegue de instancia única. Para
 * despliegues horizontales debe reemplazarse por un almacén compartido (Redis o tabla).</p>
 */
@Service
public class RevokedTokenService {

    private static final Duration EXPIRACION_POR_DEFECTO = Duration.ofDays(1);

    private final Map<String, Instant> revocados = new ConcurrentHashMap<>();

    public void revocar(String jti, Date expiracion) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        Instant limite = (expiracion != null)
                ? expiracion.toInstant()
                : Instant.now().plus(EXPIRACION_POR_DEFECTO);
        revocados.put(jti, limite);
    }

    public boolean estaRevocado(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Instant limite = revocados.get(jti);
        if (limite == null) {
            return false;
        }
        if (!limite.isAfter(Instant.now())) {
            revocados.remove(jti);
            return false;
        }
        return true;
    }

    @Scheduled(fixedDelay = 3_600_000L)
    public void limpiarExpirados() {
        Instant ahora = Instant.now();
        revocados.entrySet().removeIf(entrada -> !entrada.getValue().isAfter(ahora));
    }
}
