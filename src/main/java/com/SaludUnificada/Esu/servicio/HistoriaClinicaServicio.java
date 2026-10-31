package com.SaludUnificada.Esu.servicio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.error.ReglaNegocioException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.SaludUnificada.Esu.dto.request.HistoriaClinicaDtoRequest;
import com.SaludUnificada.Esu.dto.response.HistoriaClinicaDtoResponse;
import com.SaludUnificada.Esu.entidad.HistoriaClinica;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.HistoriaClinicaMapper;
import com.SaludUnificada.Esu.repositorio.HistoriaClinicaRepositorio;
import com.SaludUnificada.Esu.entidad.LogAccesoHc;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.LogAccesoHcRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.OwnershipSecurityEvaluator;
import com.SaludUnificada.Esu.seguridad.RequestIpUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HistoriaClinicaServicio implements IHistoriaClinicaServicio {

    @Autowired
    private HistoriaClinicaRepositorio historiaClinicaRepositorio;

    @Autowired
    private HistoriaClinicaMapper historiaClinicaMapper;

    @Autowired(required = false)
    private LogAccesoHcRepositorio logAccesoHcRepositorio;

    @Autowired(required = false)
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired(required = false)
    private PacienteRepositorio pacienteRepositorio;

    @Autowired(required = false)
    private RequestIpUtil requestIpUtil;

    @Autowired(required = false)
    private OwnershipSecurityEvaluator ownershipSecurityEvaluator;

    @Autowired(required = false)
    private NotificacionUsuarioServicio notificacionUsuarioServicio;

    @Autowired(required = false)
    private ProfesionalRepositorio profesionalRepositorio;

    @Override
    @Transactional
    public HistoriaClinicaDtoResponse crearHistoriaClinica(HistoriaClinicaDtoRequest dto) {
        if (dto == null || dto.getPacienteId() == null) {
            throw new IllegalArgumentException("El ID del paciente es obligatorio para crear la historia clínica.");
        }
        if (pacienteRepositorio != null && !pacienteRepositorio.existsById(dto.getPacienteId())) {
            throw new NoEncontradoExcepcion("Paciente no encontrado con ID: " + dto.getPacienteId());
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                if (profesionalRepositorio != null) {
                    var profOpt = profesionalRepositorio.findByUsuarioEmail(auth.getName());
                    if (profOpt.isEmpty()) {
                        throw new AccessDeniedException("El usuario autenticado no posee un perfil profesional activo vinculado.");
                    }
                    if (!Boolean.TRUE.equals(profOpt.get().getEstadoActivo())) {
                        throw new AccessDeniedException("El perfil del profesional autenticado se encuentra inactivo.");
                    }
                }
                if (ownershipSecurityEvaluator != null && !ownershipSecurityEvaluator.canAccessPatient(dto.getPacienteId())) {
                    throw new AccessDeniedException("No posee relación asistencial autorizada con el paciente para registrar historias clínicas.");
                }
            }
        } else {
            throw new AccessDeniedException("Debe autenticarse para crear una historia clínica.");
        }

        HistoriaClinica historiaClinica = historiaClinicaMapper.paraEntidad(dto);
        HistoriaClinica guardada = historiaClinicaRepositorio.save(historiaClinica);
        return historiaClinicaMapper.paraDto(guardada);
    }

    @Override
    public HistoriaClinicaDtoResponse obtenerHistoriaClinicaPorId(Long id) {
        HistoriaClinica historiaClinica = historiaClinicaRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Historia clínica no encontrada con ID: " + id));
        return historiaClinicaMapper.paraDto(historiaClinica);
    }

    @Override
    public List<HistoriaClinicaDtoResponse> listarTodas() {
        return historiaClinicaRepositorio.findAll().stream()
                .map(historiaClinicaMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<HistoriaClinicaDtoResponse> listarTodasPaginado(Pageable pageable) {
        return historiaClinicaRepositorio.findAll(pageable)
                .map(historiaClinicaMapper::paraDto);
    }

    @Override
    public Page<HistoriaClinicaDtoResponse> buscarPorRangoFechasPaginado(LocalDateTime inicio, LocalDateTime fin, Pageable pageable) {
        return historiaClinicaRepositorio.findByFechaAtencionBetween(inicio, fin, pageable)
                .map(historiaClinicaMapper::paraDto);
    }

    @Override
    @Transactional
    public void eliminarHistoriaClinica(Long id) {
        if (!historiaClinicaRepositorio.existsById(id)) {
            throw new NoEncontradoExcepcion("Historia clínica no encontrada con ID: " + id);
        }
        log.warn("Intento de eliminación física bloqueado para Historia Clínica ID={}. Aplicando principio de inalterabilidad Ley 26.529.", id);
        throw new ReglaNegocioException("Por disposición de la Ley 26.529 de Derechos del Paciente, las historias clínicas no admiten borrado físico. Cualquier rectificación debe realizarse mediante adendas o notas aclaratorias auditables.");
    }

    @Override
    public List<HistoriaClinicaDtoResponse> buscarPorPacienteId(Long pacienteId) {
        List<HistoriaClinica> historiasClinicas = historiaClinicaRepositorio.findByPacienteId(pacienteId);

        if (historiasClinicas.isEmpty()) {
            throw new NoEncontradoExcepcion("No se encontraron historias clínicas para el paciente con ID: " + pacienteId);
        }

        return historiasClinicas.stream()
                .map(historiaClinicaMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<HistoriaClinicaDtoResponse> buscarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        return historiaClinicaRepositorio.findByFechaAtencionBetween(inicio, fin).stream()
                .filter(hc -> hc.getPaciente() != null && 
                        (ownershipSecurityEvaluator == null || ownershipSecurityEvaluator.canAccessPatient(hc.getPaciente().getId())))
                .map(historiaClinicaMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public long contarPorPaciente(Long pacienteId) {
        return historiaClinicaRepositorio.countByPacienteId(pacienteId);
    }

    @Override
    public Page<HistoriaClinicaDtoResponse> listarPorPacientePaginado(Long pacienteId, Pageable pageable) {
        return historiaClinicaRepositorio.findByPacienteId(pacienteId, pageable)
                .map(historiaClinicaMapper::paraDto);
    }

    @Override
    @Transactional
    public void registrarAccesoEmergencia(Long pacienteId, String emailProfesional, String motivo, HttpServletRequest request) {
        if (emailProfesional == null || emailProfesional.isBlank()) {
            throw new AccessDeniedException("Debe autenticarse como profesional médico o administrador para registrar un acceso de emergencia.");
        }
        if (motivo == null || motivo.trim().length() < 10) {
            throw new IllegalArgumentException("Debe especificarse un motivo clínico válido y detallado para el acceso de emergencia (mínimo 10 caracteres).");
        }
        if (pacienteRepositorio != null && !pacienteRepositorio.existsById(pacienteId)) {
            throw new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId);
        }
        if (usuarioRepositorio != null && logAccesoHcRepositorio != null) {
            Usuario usuario = usuarioRepositorio.findByEmail(emailProfesional)
                    .orElseThrow(() -> new NoEncontradoExcepcion("Usuario facultativo no encontrado"));

            String rol = usuario.getRol();
            if (!"MEDICO".equalsIgnoreCase(rol) && !"PROFESIONAL".equalsIgnoreCase(rol) && !"ADMIN".equalsIgnoreCase(rol)) {
                throw new AccessDeniedException("El usuario autenticado no posee rol facultativo (médico/profesional/admin) para activar acceso de emergencia.");
            }

            String ip = requestIpUtil != null ? requestIpUtil.obtenerIpCliente(request) : (request != null ? request.getRemoteAddr() : "127.0.0.1");
            LogAccesoHc logAcceso = new LogAccesoHc(usuario.getId(), pacienteId, "ACCESO_EMERGENCIA", ip, rol, motivo.trim());
            LogAccesoHc guardado = logAccesoHcRepositorio.save(logAcceso);

            log.warn("ALERTA DE SEGURIDAD AUDITABLE: Acceso de emergencia/break-glass activado para paciente ID={} por usuario={} (rol={}) desde IP={}. EventoAuditId={}. Resultado=PERMITIDO_AUDITADO",
                    pacienteId, emailProfesional, rol, ip, (guardado != null ? guardado.getId() : null));
        }

        if (pacienteRepositorio != null && notificacionUsuarioServicio != null) {
            pacienteRepositorio.findById(pacienteId).ifPresent(p -> {
                if (p.getUsuario() != null) {
                    notificacionUsuarioServicio.crearNotificacion(
                            p.getUsuario(),
                            "Acceso de Emergencia a Historia Clínica",
                            "Se ha registrado un acceso facultativo de guardia/emergencia médica a su historia clínica bajo protocolo legal.",
                            "HISTORIA_CLINICA",
                            "/mi-historia-clinica"
                    );
                }
            });
        }
    }

    @Override
    public Page<LogAccesoHc> listarAccesosEmergenciaPaginado(Pageable pageable) {
        if (logAccesoHcRepositorio == null) {
            return Page.empty(pageable);
        }
        return logAccesoHcRepositorio.findByAccionOrderByFechaHoraDesc("ACCESO_EMERGENCIA", pageable);
    }
}

