package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.LogAuditoriaSeguridadDtoResponse;
import com.SaludUnificada.Esu.entidad.LogAuditoriaSeguridad;
import com.SaludUnificada.Esu.repositorio.LogAuditoriaSeguridadRepositorio;
import com.SaludUnificada.Esu.seguridad.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaSeguridadServicio {

    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final int VENTANA_MINUTOS = 10;

    private final LogAuditoriaSeguridadRepositorio auditoriaRepositorio;
    private final RequestIpUtil requestIpUtil;

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
        logEntry.setDetalle(motivo != null ? motivo : "Credenciales inválidas.");
        logEntry.setFecha(LocalDateTime.now());

        auditoriaRepositorio.save(logEntry);
        log.warn("LOGIN_FAIL: Intento fallido para '{}' desde IP '{}'. Motivo: {}", email, ip, motivo);
    }

    public boolean estaBloqueadoPorIntentosFallidos(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        LocalDateTime ventana = LocalDateTime.now().minusMinutes(VENTANA_MINUTOS);
        List<LogAuditoriaSeguridad> eventos = auditoriaRepositorio.findEventosRecientesPorEmail(email.trim().toLowerCase(), ventana);

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

    public Page<LogAuditoriaSeguridadDtoResponse> listarAuditoria(Pageable pageable) {
        return auditoriaRepositorio.findAllByOrderByFechaDesc(pageable).map(this::toDto);
    }

    public LogAuditoriaSeguridadDtoResponse toDto(LogAuditoriaSeguridad entity) {
        if (entity == null) return null;
        return LogAuditoriaSeguridadDtoResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .evento(entity.getEvento() != null ? entity.getEvento().name() : "")
                .detalle(entity.getDetalle())
                .fecha(entity.getFecha())
                .build();
    }

    private String obtenerClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && requestIpUtil != null) {
                return requestIpUtil.obtenerIpCliente(attributes.getRequest());
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

    @Transactional
    public boolean puedeReenviarVerificacion(String email) {
        String ip = obtenerClientIp();
        if (email == null || email.isBlank()) {
            return false;
        }
        String cleanEmail = email.trim().toLowerCase();
        LocalDateTime ventanaEmail = LocalDateTime.now().minusSeconds(60);
        if (auditoriaRepositorio.existsByEmailAndEventoAndFechaGreaterThanEqual(cleanEmail, LogAuditoriaSeguridad.EventoSeguridad.REENVIO_VERIFICACION, ventanaEmail)) {
            return false;
        }

        LocalDateTime ventanaIp = LocalDateTime.now().minusSeconds(15);
        if (auditoriaRepositorio.existsByIpAddressAndEventoAndFechaGreaterThanEqual(ip, LogAuditoriaSeguridad.EventoSeguridad.REENVIO_VERIFICACION, ventanaIp)) {
            return false;
        }

        LogAuditoriaSeguridad logEntry = new LogAuditoriaSeguridad();
        logEntry.setEmail(cleanEmail);
        logEntry.setIpAddress(ip);
        logEntry.setUserAgent(obtenerUserAgent());
        logEntry.setEvento(LogAuditoriaSeguridad.EventoSeguridad.REENVIO_VERIFICACION);
        logEntry.setDetalle("Reenvío de correo de verificación solicitado.");
        logEntry.setFecha(LocalDateTime.now());
        auditoriaRepositorio.save(logEntry);

        return true;
    }

    @Transactional
    public boolean puedeSolicitarRecuperacion(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String cleanEmail = email.trim().toLowerCase();
        LocalDateTime ventana = LocalDateTime.now().minusSeconds(60);
        if (auditoriaRepositorio.existsByEmailAndEventoAndFechaGreaterThanEqual(cleanEmail, LogAuditoriaSeguridad.EventoSeguridad.SOLICITUD_RECUPERACION, ventana)) {
            return false;
        }

        LogAuditoriaSeguridad logEntry = new LogAuditoriaSeguridad();
        logEntry.setEmail(cleanEmail);
        logEntry.setIpAddress(obtenerClientIp());
        logEntry.setUserAgent(obtenerUserAgent());
        logEntry.setEvento(LogAuditoriaSeguridad.EventoSeguridad.SOLICITUD_RECUPERACION);
        logEntry.setDetalle("Solicitud de recuperación de contraseña.");
        logEntry.setFecha(LocalDateTime.now());
        auditoriaRepositorio.save(logEntry);

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
