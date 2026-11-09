package com.SaludUnificada.Esu.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limitador de tasa y protección anti-bruteforce para operaciones de autenticación de dos factores (2FA).
 * Protege contra fuerza bruta sobre códigos TOTP de 6 dígitos y saturación de endpoints de configuración.
 */
@Slf4j
@Component
public class TwoFactorRateLimiter {

    public static final long DEFAULT_WINDOW_MS = 60_000L; // 1 minuto
    public static final int DEFAULT_MAX_CHALLENGE = 5;
    public static final int DEFAULT_MAX_CONFIG = 10;
    public static final int MAX_TRACKED_ENTRIES = 10_000;

    private final RequestIpUtil requestIpUtil;
    private final long windowMs;
    private final int maxChallengeAttempts;
    private final int maxConfigAttempts;

    private final Map<String, AttemptWindow> cache;

    public static class AttemptWindow {
        private long windowStart;
        private final AtomicInteger count;

        public AttemptWindow(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }

        public long getWindowStart() {
            return windowStart;
        }

        public int getCount() {
            return count.get();
        }
    }

    @Autowired
    public TwoFactorRateLimiter(
            RequestIpUtil requestIpUtil,
            @Value("${esu.security.2fa.rate-limit.window-ms:60000}") long windowMs,
            @Value("${esu.security.2fa.rate-limit.max-challenge:5}") int maxChallengeAttempts,
            @Value("${esu.security.2fa.rate-limit.max-config:10}") int maxConfigAttempts) {
        this.requestIpUtil = requestIpUtil;
        this.windowMs = windowMs;
        this.maxChallengeAttempts = maxChallengeAttempts;
        this.maxConfigAttempts = maxConfigAttempts;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<String, AttemptWindow>(100, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, AttemptWindow> eldest) {
                return size() > MAX_TRACKED_ENTRIES;
            }
        });
    }

    public boolean permitirChallenge(HttpServletRequest request, String identificador) {
        String key = "challenge:" + resolverClave(request, identificador);
        return permitir(key, maxChallengeAttempts);
    }

    public boolean permitirSetup(HttpServletRequest request, String identificador) {
        String key = "setup:" + resolverClave(request, identificador);
        return permitir(key, maxConfigAttempts);
    }

    public boolean permitirDisable(HttpServletRequest request, String identificador) {
        String key = "disable:" + resolverClave(request, identificador);
        return permitir(key, maxConfigAttempts);
    }

    public void limpiarIntentosChallenge(HttpServletRequest request, String identificador) {
        String key = "challenge:" + resolverClave(request, identificador);
        cache.remove(key);
    }

    private String resolverClave(HttpServletRequest request, String identificador) {
        String ip = (requestIpUtil != null && request != null)
                ? requestIpUtil.obtenerIpCliente(request)
                : (request != null ? request.getRemoteAddr() : "unknown");
        String id = (identificador != null && !identificador.isBlank()) ? identificador.trim().toLowerCase() : "anon";
        return ip + ":" + id;
    }

    private boolean permitir(String key, int maxAllowed) {
        long now = System.currentTimeMillis();
        AttemptWindow window;

        synchronized (cache) {
            window = cache.get(key);
            if (window == null || (now - window.windowStart > windowMs)) {
                window = new AttemptWindow(now, 1);
                cache.put(key, window);
                return true;
            }
        }

        int actual = window.count.incrementAndGet();
        if (actual > maxAllowed) {
            log.warn("Rate limit de 2FA excedido para clave '{}': {} intentos en ventana de {} ms (límite: {})",
                    key, actual, windowMs, maxAllowed);
            return false;
        }
        return true;
    }
}
