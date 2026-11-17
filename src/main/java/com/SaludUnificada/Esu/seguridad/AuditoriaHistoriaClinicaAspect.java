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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Aspect
@Component
public class AuditoriaHistoriaClinicaAspect {

    private static final int MAX_ROL_USUARIO_LEN = 50;
    private static final int MAX_IP_LEN = 50;
    private static final int MAX_ACCION_LEN = 100;

    @Autowired
    private LogAccesoHcRepositorio logAccesoHcRepositorio;
    @Autowired
    private com.SaludUnificada.Esu.repositorio.UsuarioRepositorio usuarioRepositorio;

    @Around("execution(* com.SaludUnificada.Esu.servicio.EvolucionMedicaServicio.obtener*(..)) || execution(* com.SaludUnificada.Esu.servicio.HistoriaClinicaServicio.obtener*(..))")
    public Object auditarYControlarAcceso(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String rol = "ANONYMOUS";
        Long usuarioId = 0L;
        String ipAddress = "127.0.0.1";

        if (auth != null && auth.isAuthenticated()) {
            if (usuarioRepositorio != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                var optUser = usuarioRepositorio.findByEmail(auth.getName());
                if (optUser.isPresent()) {
                    usuarioId = optUser.get().getId();
                }
            }
            List<String> roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a != null && a.startsWith("ROLE_"))
                    .toList();

            if (!roles.isEmpty()) {
                rol = String.join(",", roles);
            } else {
                rol = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(a -> a != null && !a.isBlank())
                        .collect(Collectors.joining(","));
                if (rol.isBlank()) {
                    rol = "USER";
                }
            }

            boolean esRecepcionista = roles.stream().anyMatch("ROLE_RECEPCIONISTA"::equalsIgnoreCase);
            boolean esAdminSinRolClinico = roles.stream().anyMatch("ROLE_ADMIN"::equalsIgnoreCase)
                    && roles.stream().noneMatch(r -> "ROLE_PROFESIONAL".equalsIgnoreCase(r) || "ROLE_MEDICO".equalsIgnoreCase(r));

            if (esRecepcionista || esAdminSinRolClinico) {
                throw new AccessDeniedException("403 Forbidden: Secreto profesional médico. El perfil actual no tiene autorización para acceder a evoluciones clínicas confidenciales.");
            }
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String remoteAddr = req.getHeader("X-Forwarded-For");
            if (remoteAddr != null && !remoteAddr.isBlank()) {
                remoteAddr = remoteAddr.split(",")[0].trim();
            } else {
                remoteAddr = req.getRemoteAddr();
            }
            if (remoteAddr != null && !remoteAddr.isBlank()) {
                ipAddress = remoteAddr;
            }
        }

        Long pacienteId = 0L;
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0 && args[0] instanceof Long) {
            pacienteId = (Long) args[0];
        }

        Object resultado = joinPoint.proceed();

        try {
            LogAccesoHc log = new LogAccesoHc(
                usuarioId,
                pacienteId,
                truncar(joinPoint.getSignature().getName(), MAX_ACCION_LEN),
                truncar(ipAddress, MAX_IP_LEN),
                truncar(rol, MAX_ROL_USUARIO_LEN)
            );
            logAccesoHcRepositorio.save(log);
        } catch (Exception ex) {
            System.err.println("Advertencia al guardar log de auditoría HC: " + ex.getMessage());
        }

        return resultado;
    }

    private String truncar(String valor, int maxLen) {
        if (valor == null) {
            return null;
        }
        return valor.length() > maxLen ? valor.substring(0, maxLen) : valor;
    }
}
