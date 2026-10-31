package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.repositorio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component("securityPolicy")
@RequiredArgsConstructor
public class OwnershipSecurityEvaluator {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final DisponibilidadProfesionalRepositorio disponibilidadRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final TurnoRepositorio turnoRepositorio;
    private final RecetaDigitalRepositorio recetaRepositorio;
    private final EstudioAdjuntoRepositorio estudioRepositorio;
    private final HistoriaClinicaRepositorio historiaClinicaRepositorio;
    private final AlergiaPacienteRepositorio alergiaRepositorio;
    private final ConsentimientoInformadoRepositorio consentimientoRepositorio;
    private final LogAccesoHcRepositorio logAccesoHcRepositorio;

    private final EvolucionMedicaRepositorio evolucionMedicaRepositorio;
    private final VinculoFamiliarRepositorio vinculoFamiliarRepositorio;

    @Autowired(required = false)
    private java.time.Clock clock;

    @Autowired(required = false)
    private java.time.ZoneId turnosZoneId;

    private LocalDateTime ahora() {
        java.time.ZoneId zone = turnosZoneId != null ? turnosZoneId : java.time.ZoneId.of("America/Argentina/Cordoba");
        java.time.Clock c = clock != null ? clock.withZone(zone) : java.time.Clock.system(zone);
        return LocalDateTime.now(c);
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean isReceptionist(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RECEPCIONISTA"));
    }

    private boolean isDoctor(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_MEDICO") ||
                a.getAuthority().equals("ROLE_PROFESIONAL"));
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Optional<Usuario> getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }
        return usuarioRepositorio.findByEmail(auth.getName());
    }

    /**
     * Acceso clínico a datos del paciente (Historia Clínica, Evoluciones, Estudios, Alergias).
     * Solo permitido para:
     * - Administradores
     * - El propio paciente
     * - Profesionales con relación asistencial autorizada (turno previo, receta emitida o acceso de emergencia auditado).
     * Recepción NO tiene acceso clínico.
     */
    public boolean canAccessPatient(Long pacienteId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        // Fase 1: El administrador general NO tiene acceso a datos clínicos confidenciales de pacientes (Ley 25.326 y 26.529)
        if (isAdmin(auth)) return false;

        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        Usuario usuario = usuarioOpt.get();

        if (isDoctor(auth)) {
            Optional<Profesional> profOpt = profesionalRepositorio.findByUsuarioEmail(usuario.getEmail());
            if (profOpt.isEmpty()) return false;
            Profesional prof = profOpt.get();
            if (!Boolean.TRUE.equals(prof.getEstadoActivo())) return false;

            // 1. Relación asistencial por turnos
            boolean hasAppointment = turnoRepositorio.existsByProfesionalIdAndPacienteId(prof.getId(), pacienteId);
            if (hasAppointment) return true;

            // 2. Relación asistencial por recetas emitidas
            boolean hasPrescription = recetaRepositorio.existsByProfesionalIdAndPacienteId(prof.getId(), pacienteId);
            if (hasPrescription) return true;

            // 3. Acceso de emergencia auditado activo en las últimas 24 horas
            LocalDateTime ventanaEmergencia = ahora().minusHours(24);
            return logAccesoHcRepositorio.existsByUsuarioIdAndPacienteIdAndAccionAndFechaHoraGreaterThanEqual(
                    usuario.getId(), pacienteId, "ACCESO_EMERGENCIA", ventanaEmergencia
            );
        }

        // Rol Paciente: solo a su propio expediente
        Optional<Paciente> pacOpt = pacienteRepositorio.findByUsuarioEmail(usuario.getEmail());
        return pacOpt.isPresent() && pacOpt.get().getId().equals(pacienteId);
    }

    /**
     * Acceso administrativo (padrón, asignación de turnos, check-in, datos de contacto).
     * Permitido para: Administradores, Recepcionistas, Profesionales autorizados y el propio Paciente.
     */
    public boolean canAccessPatientAdmin(Long pacienteId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth) || isReceptionist(auth)) return true;
        return canAccessPatient(pacienteId);
    }

    public boolean isCurrentPatient(Long pacienteId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        Optional<Paciente> pacOpt = pacienteRepositorio.findByUsuarioEmail(usuarioOpt.get().getEmail());
        return pacOpt.isPresent() && pacOpt.get().getId().equals(pacienteId);
    }

    public boolean isDoctorAuthor(Long profesionalId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;
        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        Optional<Profesional> profOpt = profesionalRepositorio.findByUsuarioEmail(usuarioOpt.get().getEmail());
        return profOpt.isPresent() && profOpt.get().getId().equals(profesionalId);
    }

    public boolean canAccessDisponibilidad(Long dispId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;
        if (disponibilidadRepositorio == null) return false;
        Optional<DisponibilidadProfesional> dispOpt = disponibilidadRepositorio.findById(dispId);
        if (dispOpt.isEmpty() || dispOpt.get().getProfesional() == null) return false;
        return isDoctorAuthor(dispOpt.get().getProfesional().getId());
    }

    public boolean isTurnoDoctor(Long turnoId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;
        Optional<Turno> turnoOpt = turnoRepositorio.findById(turnoId);
        if (turnoOpt.isEmpty() || turnoOpt.get().getProfesional() == null) return false;
        return isDoctorAuthor(turnoOpt.get().getProfesional().getId());
    }

    public boolean canAccessTurno(Long turnoId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isReceptionist(auth) || isAdmin(auth)) return true;

        Optional<Turno> turnoOpt = turnoRepositorio.findById(turnoId);
        if (turnoOpt.isEmpty()) return false;
        Turno turno = turnoOpt.get();

        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        Usuario usuario = usuarioOpt.get();

        if (isDoctor(auth)) {
            return turno.getProfesional() != null &&
                   turno.getProfesional().getUsuario() != null &&
                   turno.getProfesional().getUsuario().getEmail().equalsIgnoreCase(usuario.getEmail());
        }

        // Paciente:
        return turno.getPaciente() != null &&
               turno.getPaciente().getUsuario() != null &&
               turno.getPaciente().getUsuario().getEmail().equalsIgnoreCase(usuario.getEmail());
    }

    public boolean canAccessReceta(Long recetaId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FARMACIA"))) {
            return true;
        }
        if (isAdmin(auth)) return false;

        Optional<RecetaDigital> recetaOpt = recetaRepositorio.findById(recetaId);
        if (recetaOpt.isEmpty()) return false;
        RecetaDigital receta = recetaOpt.get();

        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        Usuario usuario = usuarioOpt.get();

        if (isDoctor(auth)) {
            return receta.getProfesional() != null &&
                   receta.getProfesional().getUsuario() != null &&
                   receta.getProfesional().getUsuario().getEmail().equalsIgnoreCase(usuario.getEmail());
        }

        return receta.getPaciente() != null &&
               receta.getPaciente().getUsuario() != null &&
               receta.getPaciente().getUsuario().getEmail().equalsIgnoreCase(usuario.getEmail());
    }

    public boolean canAccessEstudio(Long estudioId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return false;

        Optional<EstudioAdjunto> estudioOpt = estudioRepositorio.findById(estudioId);
        if (estudioOpt.isEmpty()) return false;
        EstudioAdjunto estudio = estudioOpt.get();

        return canAccessPatient(estudio.getPacienteId());
    }

    public boolean canAccessHistoriaClinica(Long hcId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return false;

        Optional<HistoriaClinica> hcOpt = historiaClinicaRepositorio.findById(hcId);
        if (hcOpt.isEmpty()) return false;
        HistoriaClinica hc = hcOpt.get();

        if (hc.getPaciente() == null) return false;
        return canAccessPatient(hc.getPaciente().getId());
    }

    public boolean canAccessConsentimiento(Long consentimientoId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return false;

        Optional<ConsentimientoInformado> consentOpt = consentimientoRepositorio.findById(consentimientoId);
        if (consentOpt.isEmpty()) return false;
        ConsentimientoInformado consent = consentOpt.get();

        if (isDoctor(auth)) {
            if (isDoctorAuthor(consent.getProfesionalId())) {
                return true;
            }
            return canAccessPatient(consent.getPacienteId());
        }

        return canAccessPatient(consent.getPacienteId());
    }

    public boolean canAccessAlergia(Long alergiaId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return false;

        Optional<AlergiaPaciente> alergiaOpt = alergiaRepositorio.findById(alergiaId);
        if (alergiaOpt.isEmpty()) return false;
        AlergiaPaciente alergia = alergiaOpt.get();

        return canAccessPatient(alergia.getPacienteId());
    }

    public boolean canAccessEvolucion(Long evolucionId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return false;

        if (evolucionMedicaRepositorio == null) return false;
        Optional<EvolucionMedica> evOpt = evolucionMedicaRepositorio.findById(evolucionId);
        if (evOpt.isEmpty()) return false;
        EvolucionMedica ev = evOpt.get();

        if (ev.getPaciente() == null) return false;
        return canAccessPatient(ev.getPaciente().getId());
    }

    public boolean canAccessFamiliar(Long vinculoId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth) || isReceptionist(auth)) return true;

        if (vinculoFamiliarRepositorio == null) return false;
        Optional<VinculoFamiliar> vinculoOpt = vinculoFamiliarRepositorio.findById(vinculoId);
        if (vinculoOpt.isEmpty()) return false;
        VinculoFamiliar v = vinculoOpt.get();

        if (v.getTitular() != null && isCurrentPatient(v.getTitular().getId())) {
            return true;
        }
        if (v.getFamiliarDni() != null) {
            Authentication authLocal = getAuthentication();
            Optional<Usuario> usuarioOpt = getCurrentUser(authLocal);
            if (usuarioOpt.isPresent()) {
                Optional<Paciente> pacOpt = pacienteRepositorio.findByUsuarioEmail(usuarioOpt.get().getEmail());
                if (pacOpt.isPresent() && v.getFamiliarDni().equals(pacOpt.get().getDni())) {
                    return true;
                }
            }
        }
        if (v.getTitular() != null) {
            return canAccessPatient(v.getTitular().getId());
        }
        return false;
    }
}
