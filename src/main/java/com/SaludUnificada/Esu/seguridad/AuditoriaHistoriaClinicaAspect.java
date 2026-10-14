package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.entidad.LogAccesoHc;
import com.SaludUnificada.Esu.repositorio.LogAccesoHcRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditoriaHistoriaClinicaAspect {

    @Autowired
    private LogAccesoHcRepositorio logAccesoHcRepositorio;

    @Around("execution(* com.SaludUnificada.Esu.servicio.EvolucionMedicaServicio.obtener*(..)) || execution(* com.SaludUnificada.Esu.servicio.HistoriaClinicaServicio.obtener*(..))")
    public Object auditarYControlarAcceso(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String rol = "ANONYMOUS";
        Long usuarioId = 0L;
        String ipAddress = "127.0.0.1";

        if (auth != null && auth.isAuthenticated()) {
            rol = auth.getAuthorities().stream()
                .map(Object::toString)
                .reduce((a, b) -> a + "," + b)
                .orElse("USER");

            // Control estricto de privacidad y secreto médico (HU-79 DoD):
            // Si el usuario es RECEPCIONISTA o ADMIN sin rol médico intentando acceder al expediente íntimo:
            if (rol.contains("RECEPCIONISTA") || (rol.contains("ADMIN") && !rol.contains("PROFESIONAL"))) {
                // Bloqueo con 403 Forbidden por secreto médico
                throw new AccessDeniedException("403 Forbidden: Secreto profesional médico. El perfil actual no tiene autorización para acceder a evoluciones clínicas confidenciales.");
            }
        }

        // Obtener IP del cliente
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String remoteAddr = req.getHeader("X-Forwarded-For");
            if (remoteAddr == null || remoteAddr.isEmpty()) {
                remoteAddr = req.getRemoteAddr();
            }
            if (remoteAddr != null) {
                ipAddress = remoteAddr;
            }
        }

        // Obtener pacienteId de los argumentos si existe
        Long pacienteId = 0L;
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0 && args[0] instanceof Long) {
            pacienteId = (Long) args[0];
        }

        // Ejecutar el método objetivo
        Object resultado = joinPoint.proceed();

        // Registrar en bitácora de auditoría inmutable
        try {
            LogAccesoHc log = new LogAccesoHc(
                usuarioId,
                pacienteId,
                joinPoint.getSignature().getName(),
                ipAddress,
                rol
            );
            logAccesoHcRepositorio.save(log);
        } catch (Exception ex) {
            System.err.println("Advertencia al guardar log de auditoría HC: " + ex.getMessage());
        }

        return resultado;
    }
}
