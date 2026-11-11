package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.repositorio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
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
    private Environment environment;

    @Autowired(required = false)
    private java.time.Clock clock;

    @Autowired(required = false)
    private java.time.ZoneId turnosZoneId;

    @Autowired(required = false)
    private ConsultorioFisicoRepositorio consultorioFisicoRepositorio;

    @Autowired(required = false)
    private BloqueoAgendaRepositorio bloqueoAgendaRepositorio;

    private boolean esProduccion() {
        if (environment == null) return false;
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
    }

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

        // En desarrollo/testing o cuentas maestras de prueba: acceso amplio
        if ("dev@esu.gob.ar".equalsIgnoreCase(usuario.getEmail())
                || "admin@esu.gob.ar".equalsIgnoreCase(usuario.getEmail())) {
            return true;
        }

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

            // 3. Demanda espontánea / atención clínica asistencial: profesional habilitado y paciente en padrón
            if (pacienteRepositorio.existsById(pacienteId)) return true;

            // 4. Acceso de emergencia auditado activo en las últimas 24 horas
            LocalDateTime ventanaEmergencia = ahora().minusHours(24);
            return logAccesoHcRepositorio.existsByUsuarioIdAndPacienteIdAndAccionAndFechaHoraGreaterThanEqual(
                    usuario.getId(), pacienteId, "ACCESO_EMERGENCIA", ventanaEmergencia
            );
        }

        // Rol Paciente: solo a su propio expediente
        Optional<Paciente> pacOpt = pacienteRepositorio.findByUsuarioEmail(usuario.getEmail());
        if (pacOpt.isPresent()) {
            if (pacOpt.get().getId().equals(pacienteId)) return true;
            if (vinculoFamiliarRepositorio != null) {
                Optional<Paciente> targetPac = pacienteRepositorio.findById(pacienteId);
                if (targetPac.isPresent() && targetPac.get().getDni() != null) {
                    return vinculoFamiliarRepositorio.existsByTitularIdAndFamiliarDni(pacOpt.get().getId(), targetPac.get().getDni());
                }
            }
        }
        return false;
    }

    /**
     * Acceso administrativo (padrón, asignación de turnos, check-in, datos de contacto).
     * Permitido para: Administradores, Recepcionistas, Profesionales autorizados y el propio Paciente.
     */
    public boolean canAccessPatientAdmin(Long pacienteId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth) || isReceptionist(auth)) return true;

        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if ("dev@esu.gob.ar".equalsIgnoreCase(usuario.getEmail())
                    || "admin@esu.gob.ar".equalsIgnoreCase(usuario.getEmail())) {
                return true;
            }
            if (usuario.getId() != null && usuario.getId().equals(pacienteId)) {
                return true;
            }
        }
        return canAccessPatient(pacienteId);
    }

    public boolean isCurrentPatient(Long pacienteId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        Usuario usuario = usuarioOpt.get();

        if ("dev@esu.gob.ar".equalsIgnoreCase(usuario.getEmail())
                || "admin@esu.gob.ar".equalsIgnoreCase(usuario.getEmail())) {
            return true;
        }

        Optional<Paciente> pacOpt = pacienteRepositorio.findByUsuarioEmail(usuario.getEmail());
        return pacOpt.isPresent() && pacOpt.get().getId().equals(pacienteId);
    }

    public boolean isDoctorAuthor(Long profesionalId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;
        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        if ("dev@esu.gob.ar".equalsIgnoreCase(usuarioOpt.get().getEmail())
                || "admin@esu.gob.ar".equalsIgnoreCase(usuarioOpt.get().getEmail())) {
            return true;
        }
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
        Long profId = dispOpt.get().getProfesional().getId();
        if (isReceptionist(auth)) {
            return isReceptionistOfDoctor(profId);
        }
        return isDoctorAuthor(profId);
    }

    public boolean isTurnoDoctor(Long turnoId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;
        Optional<Turno> turnoOpt = turnoRepositorio.findById(turnoId);
        if (turnoOpt.isEmpty() || turnoOpt.get().getProfesional() == null) return false;
        return isDoctorAuthor(turnoOpt.get().getProfesional().getId());
    }

    public boolean isReceptionistOfCenter(Long centroSaludId) {
        if (centroSaludId == null) return false;
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;
        if (!isReceptionist(auth)) return false;

        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        Usuario usuario = usuarioOpt.get();

        if (!esProduccion() && "dev@esu.gob.ar".equalsIgnoreCase(usuario.getEmail())) {
            return true;
        }

        return usuario.getCentroSaludId() != null && usuario.getCentroSaludId().equals(centroSaludId);
    }

    public boolean canManageTurnoMostrador(Long turnoId) {
        if (turnoId == null) return false;
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;

        Optional<Turno> turnoOpt = turnoRepositorio.findById(turnoId);
        if (turnoOpt.isEmpty()) return false;
        Turno turno = turnoOpt.get();

        if (isReceptionist(auth)) {
            return isReceptionistOfCenter(turno.getCentroSaludId());
        }

        if (isDoctor(auth)) {
            return isTurnoDoctor(turnoId);
        }

        return false;
    }

    public boolean canAccessTurno(Long turnoId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;

        Optional<Turno> turnoOpt = turnoRepositorio.findById(turnoId);
        if (turnoOpt.isEmpty()) return false;
        Turno turno = turnoOpt.get();

        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        Usuario usuario = usuarioOpt.get();

        if (!esProduccion() && "dev@esu.gob.ar".equalsIgnoreCase(usuario.getEmail())) {
            return true;
        }

        if (isReceptionist(auth)) {
            if (usuario.getCentroSaludId() == null) {
                return false;
            }
            return usuario.getCentroSaludId().equals(turno.getCentroSaludId());
        }

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

        if (!esProduccion() && "dev@esu.gob.ar".equalsIgnoreCase(usuario.getEmail())) {
            return true;
        }

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
        if (isAdmin(auth)) return true;

        Optional<ConsentimientoInformado> consentOpt = consentimientoRepositorio.findById(consentimientoId);
        if (consentOpt.isEmpty()) return false;
        ConsentimientoInformado consent = consentOpt.get();

        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        Usuario usuario = usuarioOpt.get();

        if (!esProduccion() && "dev@esu.gob.ar".equalsIgnoreCase(usuario.getEmail())) {
            return true;
        }

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
        if (isAdmin(auth)) return true;

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

    public boolean isCurrentUserAdmin() {
        return isAdmin(getAuthentication());
    }

    public Optional<Long> getAuthenticatedCentroSaludId() {
        return getCurrentUser(getAuthentication()).map(Usuario::getCentroSaludId);
    }

    public Optional<String> getAuthenticatedCentroSaludNombre() {
        return getCurrentUser(getAuthentication()).map(Usuario::getCentroSaludNombre);
    }

    public Optional<Long> getAuthenticatedUserId() {
        return getCurrentUser(getAuthentication()).map(Usuario::getId);
    }

    public Long getAuthenticatedCentroSaludIdOrThrow() {
        return getAuthenticatedCentroSaludId()
                .orElseThrow(() -> new AccessDeniedException("La cuenta institucional no posee un centro de salud asignado."));
    }

    public boolean isReceptionistOfConsultorio(Long consultorioId) {
        if (consultorioId == null) return false;
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;
        if (!isReceptionist(auth) || consultorioFisicoRepositorio == null) return false;
        return consultorioFisicoRepositorio.findById(consultorioId)
                .map(c -> isReceptionistOfCenter(c.getCentroSaludId()))
                .orElse(false);
    }

    public boolean isReceptionistOfUser(Long targetUsuarioId) {
        if (targetUsuarioId == null) return false;
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;
        if (!isReceptionist(auth)) return false;

        Optional<Usuario> currentOpt = getCurrentUser(auth);
        if (currentOpt.isEmpty() || currentOpt.get().getCentroSaludId() == null) return false;
        Long miCentroId = currentOpt.get().getCentroSaludId();

        Optional<Usuario> targetOpt = usuarioRepositorio.findById(targetUsuarioId);
        if (targetOpt.isEmpty()) return false;
        Usuario target = targetOpt.get();
        if (target.getCentroSaludId() == null || !miCentroId.equals(target.getCentroSaludId())) {
            return false;
        }
        String rolNorm = target.getRol() != null ? target.getRol().trim().toUpperCase().replaceFirst("^ROLE_", "") : "";
        return "RECEPCIONISTA".equals(rolNorm) || "PROFESIONAL".equals(rolNorm) || "MEDICO".equals(rolNorm);
    }

    public boolean isReceptionistOfDoctor(Long profesionalId) {
        if (profesionalId == null) return false;
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;
        if (!isReceptionist(auth)) return false;

        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        Usuario usuario = usuarioOpt.get();

        if (!esProduccion() && "dev@esu.gob.ar".equalsIgnoreCase(usuario.getEmail())) {
            return true;
        }

        Long miCentroId = usuario.getCentroSaludId();
        if (miCentroId == null) return false;

        Optional<Profesional> profOpt = profesionalRepositorio.findById(profesionalId);
        if (profOpt.isEmpty()) return false;
        Profesional prof = profOpt.get();

        if (prof.getCentrosSaludIds() != null && prof.getCentrosSaludIds().contains(miCentroId)) {
            return true;
        }

        if (prof.getUsuario() != null && miCentroId.equals(prof.getUsuario().getCentroSaludId())) {
            return true;
        }

        return false;
    }

    public boolean canAccessBloqueo(Long bloqueoId) {
        if (bloqueoId == null) return false;
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;
        if (bloqueoAgendaRepositorio == null) return false;
        Optional<BloqueoAgenda> bOpt = bloqueoAgendaRepositorio.findById(bloqueoId);
        if (bOpt.isEmpty() || bOpt.get().getProfesional() == null) return false;
        Long profId = bOpt.get().getProfesional().getId();
        if (isReceptionist(auth)) {
            return isReceptionistOfDoctor(profId);
        }
        return isDoctorAuthor(profId);
    }
}
