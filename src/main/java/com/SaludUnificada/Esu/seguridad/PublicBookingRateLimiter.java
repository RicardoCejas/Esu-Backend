package com.SaludUnificada.Esu.seguridad;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limitador de tasa (Rate Limiter) seguro y acotado para reservas públicas de turnos.
 * Cumple con protección contra DoS, reinicio limpio de ventana al expirar y validación
 * de proxy confiable para evitar manipulación de cabeceras de IP.
 */
@Slf4j
@Component
public class PublicBookingRateLimiter {

    public static final long DEFAULT_WINDOW_MS = 600_000L; // 10 minutos
    public static final int DEFAULT_MAX_REQUESTS = 20;
    public static final int MAX_TRACKED_IPS = 10_000;

    private final RequestIpUtil requestIpUtil;
    private final long windowMs;
    private final int maxRequests;

    // Cache LRU con tamaño acotado en memoria (previene desbordamiento de heap por IPs fraudulentas)
    private final Map<String, RateWindow> cache;

    public static class RateWindow {
        private long windowStart;
        private final AtomicInteger count;

        public RateWindow(long windowStart, int initialCount) {
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
    public PublicBookingRateLimiter(
            RequestIpUtil requestIpUtil,
            @Value("${esu.turnos.public-booking.window-ms:600000}") long windowMs,
            @Value("${esu.turnos.public-booking.max-requests:20}") int maxRequests) {
        this.requestIpUtil = requestIpUtil;
        this.windowMs = windowMs;
        this.maxRequests = maxRequests;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<String, RateWindow>(100, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, RateWindow> eldest) {
                return size() > MAX_TRACKED_IPS;
            }
        });
    }

    /**
     * Evalúa si la solicitud HTTP proveniente de la IP del cliente puede continuar.
     */
    public boolean permitirReserva(HttpServletRequest request) {
        String clientIp = (requestIpUtil != null && request != null)
                ? requestIpUtil.obtenerIpCliente(request)
                : (request != null ? request.getRemoteAddr() : "unknown");
        return permitirReserva(clientIp);
    }

    /**
     * Evalúa la IP del cliente según la ventana y el límite configurado.
     */
    public boolean permitirReserva(String clientIp) {
        long now = System.currentTimeMillis();
        RateWindow window;

        synchronized (cache) {
            window = cache.get(clientIp);
            if (window == null || (now - window.windowStart > windowMs)) {
                // Al vencer la ventana temporal o ser la primera solicitud, se reinician tanto la ventana como el contador
                window = new RateWindow(now, 1);
                cache.put(clientIp, window);
                return true;
            }
        }

        // Si se encuentra dentro de la misma ventana activa, incrementa el contador
        int actual = window.count.incrementAndGet();
        if (actual > maxRequests) {
            log.warn("Rate limit excedido para IP {}: {} peticiones en ventana de {} ms (límite: {})",
                    clientIp, actual, windowMs, maxRequests);
            return false;
        }

        return true;
    }

    public void resetParaTesting() {
        cache.clear();
    }
}
