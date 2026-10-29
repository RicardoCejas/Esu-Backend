package com.SaludUnificada.Esu.servicio;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import com.SaludUnificada.Esu.entidad.LogAuditoriaSeguridad;
import com.SaludUnificada.Esu.repositorio.LogAuditoriaSeguridadRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AuditoriaSeguridadServicio {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaSeguridadServicio.class);
    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final int VENTANA_MINUTOS = 10;

    @Autowired
    private LogAuditoriaSeguridadRepositorio auditoriaRepositorio;

    @Transactional
    public void registrarLoginExitoso(String email) {
        String ip = obtenerClientIp();
        String userAgent = obtenerUserAgent();

        LogAuditoriaSeguridad logEntry = new LogAuditoriaSeguridad();
        logEntry.setEmail(email != null ? email.trim().toLowerCase() : "desconocido");
        logEntry.setIpAddress(ip);
        logEntry.setUserAgent(userAgent);
        logEntry.setEvento(LogAuditoriaSeguridad.EventoSeguridad.LOGIN_OK);
        logEntry.setDetalle("Inicio de sesión completado satisfactoriamente.");
        logEntry.setFecha(LocalDateTime.now());

        auditoriaRepositorio.save(logEntry);
        log.info("LOGIN_OK: Usuario '{}' desde IP '{}'", email, ip);
    }

    @Transactional
    public void registrarLoginFallido(String email, String motivo) {
        String ip = obtenerClientIp();
        String userAgent = obtenerUserAgent();

        LogAuditoriaSeguridad logEntry = new LogAuditoriaSeguridad();
        logEntry.setEmail(email != null ? email.trim().toLowerCase() : "desconocido");
        logEntry.setIpAddress(ip);
        logEntry.setUserAgent(userAgent);
        logEntry.setEvento(LogAuditoriaSeguridad.EventoSeguridad.LOGIN_FAIL);
        logEntry.setDetalle(motivo != null ? motivo : "Credenciales incorrectas");
        logEntry.setFecha(LocalDateTime.now());

        auditoriaRepositorio.save(logEntry);
        log.warn("LOGIN_FAIL: Intento fallido para '{}' desde IP '{}'. Motivo: {}", email, ip, motivo);
    }

    public boolean estaBloqueadoPorIntentosFallidos(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        LocalDateTime limite = LocalDateTime.now().minusMinutes(VENTANA_MINUTOS);
        List<LogAuditoriaSeguridad> eventos = auditoriaRepositorio.findEventosRecientesPorEmail(email.trim().toLowerCase(), limite);

        int fallosConsecutivos = 0;
        for (LogAuditoriaSeguridad ev : eventos) {
            if (ev.getEvento() == LogAuditoriaSeguridad.EventoSeguridad.LOGIN_FAIL) {
                fallosConsecutivos++;
                if (fallosConsecutivos >= MAX_INTENTOS_FALLIDOS) {
                    return true;
                }
            } else if (ev.getEvento() == LogAuditoriaSeguridad.EventoSeguridad.LOGIN_OK) {
                break;
            }
        }

        return false;
    }

    public Page<LogAuditoriaSeguridad> listarAuditoria(Pageable pageable) {
        return auditoriaRepositorio.findAllByOrderByFechaDesc(pageable);
    }

    private String obtenerClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isBlank() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                    return xForwardedFor.split(",")[0].trim();
                }
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isBlank() && !"unknown".equalsIgnoreCase(xRealIp)) {
                    return xRealIp.trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    private String obtenerUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String ua = attributes.getRequest().getHeader("User-Agent");
                if (ua != null) {
                    return ua.length() > 290 ? ua.substring(0, 290) : ua;
                }
            }
        } catch (Exception ignored) {
        }
        return "Unknown-Client";
    }

    private final Map<String, Instant> ultimoReenvioVerificacion = new ConcurrentHashMap<>();
    private final Map<String, Instant> ultimaSolicitudRecuperacion = new ConcurrentHashMap<>();

    public boolean puedeReenviarVerificacion(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String key = email.trim().toLowerCase();
        Instant ahora = Instant.now();
        Instant previo = ultimoReenvioVerificacion.get(key);
        if (previo != null && Duration.between(previo, ahora).getSeconds() < 60) {
            return false;
        }
        ultimoReenvioVerificacion.put(key, ahora);
        return true;
    }

    public boolean puedeSolicitarRecuperacion(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String key = email.trim().toLowerCase();
        Instant ahora = Instant.now();
        Instant previo = ultimaSolicitudRecuperacion.get(key);
        if (previo != null && Duration.between(previo, ahora).getSeconds() < 60) {
            return false;
        }
        ultimaSolicitudRecuperacion.put(key, ahora);
        return true;
    }

    @Transactional(readOnly = true)
    public byte[] exportarAuditoriaCsv() {
        List<LogAuditoriaSeguridad> logs = auditoriaRepositorio.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fecha"));
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Fecha,Email,Evento,IP,Detalle,UserAgent\n");
        for (LogAuditoriaSeguridad l : logs) {
            sb.append(l.getId()).append(",")
                    .append(l.getFecha()).append(",")
                    .append(l.getEmail()).append(",")
                    .append(l.getEvento()).append(",")
                    .append(l.getIpAddress() != null ? l.getIpAddress() : "").append(",")
                    .append("\"").append(l.getDetalle() != null ? l.getDetalle().replace("\"", "'") : "").append("\",")
                    .append("\"").append(l.getUserAgent() != null ? l.getUserAgent().replace("\"", "'") : "").append("\"\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
