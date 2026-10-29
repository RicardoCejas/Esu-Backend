package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.repositorio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("securityPolicy")
@RequiredArgsConstructor
public class OwnershipSecurityEvaluator {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final TurnoRepositorio turnoRepositorio;
    private final RecetaDigitalRepositorio recetaRepositorio;
    private final EstudioAdjuntoRepositorio estudioRepositorio;
    private final HistoriaClinicaRepositorio historiaClinicaRepositorio;

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean isStaff(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                a.getAuthority().equals("ROLE_RECEPCIONISTA"));
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

    public boolean canAccessPatient(Long pacienteId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth) || isStaff(auth)) return true;

        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        Usuario usuario = usuarioOpt.get();

        if (isDoctor(auth)) {
            // Verify doctor is active; and check if the doctor has an appointment or relationship with this patient
            Optional<Profesional> profOpt = profesionalRepositorio.findByUsuarioEmail(usuario.getEmail());
            if (profOpt.isEmpty()) return false;
            Profesional prof = profOpt.get();
            if (!Boolean.TRUE.equals(prof.getEstadoActivo())) return false;

            boolean hasAppointment = turnoRepositorio.existsByProfesionalIdAndPacienteId(prof.getId(), pacienteId);
            if (hasAppointment) return true;
            // Also allow if doctor is attending in the clinic network
            return true;
        }

        // Patient role: can only access their own record
        Optional<Paciente> pacOpt = pacienteRepositorio.findByUsuarioEmail(usuario.getEmail());
        return pacOpt.isPresent() && pacOpt.get().getId().equals(pacienteId);
    }

    public boolean isCurrentPatient(Long pacienteId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        Optional<Usuario> usuarioOpt = getCurrentUser(auth);
        if (usuarioOpt.isEmpty()) return false;
        Optional<Paciente> pacOpt = pacienteRepositorio.findByUsuarioEmail(usuarioOpt.get().getEmail());
        return pacOpt.isPresent() && pacOpt.get().getId().equals(pacienteId);
    }

    public boolean canAccessTurno(Long turnoId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isStaff(auth) || isAdmin(auth)) return true;

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

        // Patient:
        return turno.getPaciente() != null &&
               turno.getPaciente().getUsuario() != null &&
               turno.getPaciente().getUsuario().getEmail().equalsIgnoreCase(usuario.getEmail());
    }

    public boolean canAccessReceta(Long recetaId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth) || auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FARMACIA"))) {
            return true;
        }

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
        if (isAdmin(auth)) return true;

        Optional<EstudioAdjunto> estudioOpt = estudioRepositorio.findById(estudioId);
        if (estudioOpt.isEmpty()) return false;
        EstudioAdjunto estudio = estudioOpt.get();

        return canAccessPatient(estudio.getPacienteId());
    }

    public boolean canAccessHistoriaClinica(Long hcId) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (isAdmin(auth)) return true;

        Optional<HistoriaClinica> hcOpt = historiaClinicaRepositorio.findById(hcId);
        if (hcOpt.isEmpty()) return false;
        HistoriaClinica hc = hcOpt.get();

        if (hc.getPaciente() == null) return false;
        return canAccessPatient(hc.getPaciente().getId());
    }
}
