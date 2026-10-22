package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.dto.response.HistoriaClinicaDtoResponse;
import com.SaludUnificada.Esu.dto.response.ResumenClinicoDtoResponse;
import com.SaludUnificada.Esu.entidad.EvolucionMedica;
import com.SaludUnificada.Esu.entidad.HistoriaClinica;
import com.SaludUnificada.Esu.entidad.LogAccesoHc;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.EvolucionMedicaRepositorio;
import com.SaludUnificada.Esu.repositorio.LogAccesoHcRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Issue #120: auditoria de acceso a la historia clinica.
 *
 * <p>Registra el usuario real (no 0), cubre lectura de resumenes, historias, timeline
 * y descarga de PDF, y usa la IP de la conexion en lugar de confiar en
 * {@code X-Forwarded-For}, que cualquier cliente puede falsificar.</p>
 */
@Aspect
@Component
public class AuditoriaHistoriaClinicaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaHistoriaClinicaAspect.class);

    private static final Set<String> METODOS_CON_PACIENTE_ID = Set.of(
            "obtenerResumenClinico",
            "obtenerTimelinePaciente",
            "buscarPorPacienteId"
    );

    private final LogAccesoHcRepositorio logAccesoHcRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final EvolucionMedicaRepositorio evolucionMedicaRepositorio;

    public AuditoriaHistoriaClinicaAspect(LogAccesoHcRepositorio logAccesoHcRepositorio,
                                          UsuarioRepositorio usuarioRepositorio,
                                          EvolucionMedicaRepositorio evolucionMedicaRepositorio) {
        this.logAccesoHcRepositorio = logAccesoHcRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.evolucionMedicaRepositorio = evolucionMedicaRepositorio;
    }

    @Around("execution(* com.SaludUnificada.Esu.servicio.EvolucionMedicaServicio.obtener*(..))"
            + " || execution(* com.SaludUnificada.Esu.servicio.EvolucionMedicaServicio.generarPdf*(..))"
            + " || execution(* com.SaludUnificada.Esu.servicio.HistoriaClinicaServicio.obtener*(..))"
            + " || execution(* com.SaludUnificada.Esu.servicio.HistoriaClinicaServicio.listar*(..))"
            + " || execution(* com.SaludUnificada.Esu.servicio.HistoriaClinicaServicio.buscar*(..))")
    public Object auditarYControlarAcceso(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String rol = "ANONYMOUS";
        Long usuarioId = 0L;

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            rol = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));

            // Secreto profesional: recepcion y administracion no leen el expediente clinico.
            if (rol.contains("ROLE_RECEPCIONISTA")
                    || (rol.contains("ROLE_ADMIN") && !rol.contains("ROLE_PROFESIONAL"))) {
                throw new AccessDeniedException(
                        "403 Forbidden: secreto profesional medico. El perfil actual no puede acceder a evoluciones clinicas confidenciales.");
            }

            usuarioId = usuarioRepositorio.findByEmail(auth.getName())
                    .map(Usuario::getId)
                    .orElse(0L);
        }

        String ipAddress = obtenerIpCliente();
        String accion = joinPoint.getSignature().getName();
        if (accion.length() > 100) {
            accion = accion.substring(0, 100);
        }

        Object resultado = joinPoint.proceed();

        for (Long pacienteId : extraerPacientes(joinPoint, resultado)) {
            try {
                LogAccesoHc registro = new LogAccesoHc(usuarioId, pacienteId, accion, ipAddress, rol);
                logAccesoHcRepositorio.save(registro);
            } catch (Exception ex) {
                log.warn("No se pudo guardar el log de auditoria HC: {}", ex.getMessage());
            }
        }

        return resultado;
    }

    private String obtenerIpCliente() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "127.0.0.1";
        }
        HttpServletRequest request = attrs.getRequest();
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null && !remoteAddr.isBlank() ? remoteAddr : "127.0.0.1";
    }

    private Set<Long> extraerPacientes(ProceedingJoinPoint joinPoint, Object resultado) {
        Set<Long> pacientes = new LinkedHashSet<>();

        if (resultado instanceof EvolucionMedica evolucion) {
            agregar(pacientes, evolucion.getPaciente());
        } else if (resultado instanceof HistoriaClinica historia) {
            agregar(pacientes, historia.getPaciente());
        } else if (resultado instanceof ResumenClinicoDtoResponse resumen) {
            agregar(pacientes, resumen.getPacienteId());
        } else if (resultado instanceof HistoriaClinicaDtoResponse historia) {
            agregar(pacientes, historia.getPaciente() != null ? historia.getPaciente().getId() : null);
        } else if (resultado instanceof List<?> lista) {
            for (Object item : lista) {
                if (item instanceof HistoriaClinicaDtoResponse historia) {
                    agregar(pacientes, historia.getPaciente() != null ? historia.getPaciente().getId() : null);
                }
            }
        }

        if (pacientes.isEmpty()) {
            Object[] args = joinPoint.getArgs();
            String metodo = joinPoint.getSignature().getName();
            if (args.length > 0 && args[0] instanceof Long id) {
                if (METODOS_CON_PACIENTE_ID.contains(metodo)) {
                    agregar(pacientes, id);
                } else if (metodo.startsWith("generarPdf")) {
                    evolucionMedicaRepositorio.findById(id)
                            .ifPresent(evolucion -> agregar(pacientes, evolucion.getPaciente()));
                }
            }
        }

        if (pacientes.isEmpty()) {
            // Accesos agregados (por ejemplo listados vacios) quedan registrados igualmente.
            pacientes.add(0L);
        }

        return pacientes;
    }

    private void agregar(Set<Long> pacientes, com.SaludUnificada.Esu.entidad.Paciente paciente) {
        if (paciente != null) {
            agregar(pacientes, paciente.getId());
        }
    }

    private void agregar(Set<Long> pacientes, Long pacienteId) {
        if (pacienteId != null) {
            pacientes.add(pacienteId);
        }
    }
}
