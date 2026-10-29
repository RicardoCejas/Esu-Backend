package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnershipSecurityEvaluatorTest {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;
    @Mock
    private PacienteRepositorio pacienteRepositorio;
    @Mock
    private ProfesionalRepositorio profesionalRepositorio;
    @Mock
    private TurnoRepositorio turnoRepositorio;
    @Mock
    private RecetaDigitalRepositorio recetaRepositorio;
    @Mock
    private EstudioAdjuntoRepositorio estudioRepositorio;
    @Mock
    private HistoriaClinicaRepositorio historiaClinicaRepositorio;
    @Mock
    private ConsentimientoInformadoRepositorio consentimientoRepositorio;
    @Mock
    private AlergiaPacienteRepositorio alergiaRepositorio;
    @Mock
    private LogAccesoHcRepositorio logAccesoHcRepositorio;

    @InjectMocks
    private OwnershipSecurityEvaluator evaluator;

    private Usuario adminUser;
    private Usuario doctorUser;
    private Usuario patientUser;
    private Paciente paciente;
    private Profesional profesional;

    @BeforeEach
    void setUp() {
        adminUser = new Usuario();
        adminUser.setId(1L);
        adminUser.setEmail("admin@esu.test");
        adminUser.setRol("ADMIN");

        doctorUser = new Usuario();
        doctorUser.setId(2L);
        doctorUser.setEmail("doctor@esu.test");
        doctorUser.setRol("MEDICO");

        patientUser = new Usuario();
        patientUser.setId(3L);
        patientUser.setEmail("patient@esu.test");
        patientUser.setRol("PACIENTE");

        paciente = new Paciente();
        paciente.setId(10L);
        paciente.setUsuario(patientUser);

        profesional = new Profesional();
        profesional.setId(20L);
        profesional.setEstadoActivo(true);
        profesional.setUsuario(doctorUser);
    }

    private void authenticate(String email, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                email, "N/A", List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void adminCanAccessAnyPatient() {
        authenticate("admin@esu.test", "ADMIN");
        assertTrue(evaluator.canAccessPatient(10L));
    }

    @Test
    void patientCanAccessSelf() {
        authenticate("patient@esu.test", "PACIENTE");
        when(usuarioRepositorio.findByEmail("patient@esu.test")).thenReturn(Optional.of(patientUser));
        when(pacienteRepositorio.findByUsuarioEmail("patient@esu.test")).thenReturn(Optional.of(paciente));

        assertTrue(evaluator.canAccessPatient(10L));
    }

    @Test
    void patientCannotAccessOtherPatient() {
        authenticate("patient@esu.test", "PACIENTE");
        when(usuarioRepositorio.findByEmail("patient@esu.test")).thenReturn(Optional.of(patientUser));
        when(pacienteRepositorio.findByUsuarioEmail("patient@esu.test")).thenReturn(Optional.of(paciente));

        assertFalse(evaluator.canAccessPatient(999L));
    }

    @Test
    void doctorWithoutRelationshipCannotAccessPatient() {
        authenticate("doctor@esu.test", "MEDICO");
        when(usuarioRepositorio.findByEmail("doctor@esu.test")).thenReturn(Optional.of(doctorUser));
        when(profesionalRepositorio.findByUsuarioEmail("doctor@esu.test")).thenReturn(Optional.of(profesional));
        when(turnoRepositorio.existsByProfesionalIdAndPacienteId(20L, 10L)).thenReturn(false);
        when(recetaRepositorio.existsByProfesionalIdAndPacienteId(20L, 10L)).thenReturn(false);
        when(logAccesoHcRepositorio.existsByUsuarioIdAndPacienteIdAndAccionAndFechaHoraGreaterThanEqual(
                eq(2L), eq(10L), eq("ACCESO_EMERGENCIA"), any(LocalDateTime.class))).thenReturn(false);

        assertFalse(evaluator.canAccessPatient(10L));
    }

    @Test
    void doctorWithTurnoRelationshipCanAccessPatient() {
        authenticate("doctor@esu.test", "MEDICO");
        when(usuarioRepositorio.findByEmail("doctor@esu.test")).thenReturn(Optional.of(doctorUser));
        when(profesionalRepositorio.findByUsuarioEmail("doctor@esu.test")).thenReturn(Optional.of(profesional));
        when(turnoRepositorio.existsByProfesionalIdAndPacienteId(20L, 10L)).thenReturn(true);

        assertTrue(evaluator.canAccessPatient(10L));
    }

    @Test
    void doctorWithBreakGlassEmergencyAccessCanAccessPatient() {
        authenticate("doctor@esu.test", "MEDICO");
        when(usuarioRepositorio.findByEmail("doctor@esu.test")).thenReturn(Optional.of(doctorUser));
        when(profesionalRepositorio.findByUsuarioEmail("doctor@esu.test")).thenReturn(Optional.of(profesional));
        when(turnoRepositorio.existsByProfesionalIdAndPacienteId(20L, 10L)).thenReturn(false);
        when(recetaRepositorio.existsByProfesionalIdAndPacienteId(20L, 10L)).thenReturn(false);
        when(logAccesoHcRepositorio.existsByUsuarioIdAndPacienteIdAndAccionAndFechaHoraGreaterThanEqual(
                eq(2L), eq(10L), eq("ACCESO_EMERGENCIA"), any(LocalDateTime.class))).thenReturn(true);

        assertTrue(evaluator.canAccessPatient(10L));
    }

    @Test
    void isTurnoDoctorOnlyAuthorCanModify() {
        Turno turno = new Turno();
        turno.setId(100L);
        turno.setProfesional(profesional);

        when(turnoRepositorio.findById(100L)).thenReturn(Optional.of(turno));
        when(profesionalRepositorio.findByUsuarioEmail("doctor@esu.test")).thenReturn(Optional.of(profesional));
        when(usuarioRepositorio.findByEmail("doctor@esu.test")).thenReturn(Optional.of(doctorUser));

        authenticate("doctor@esu.test", "MEDICO");
        assertTrue(evaluator.isTurnoDoctor(100L));

        Profesional otherProf = new Profesional();
        otherProf.setId(99L);
        Turno otherTurno = new Turno();
        otherTurno.setId(101L);
        otherTurno.setProfesional(otherProf);
        when(turnoRepositorio.findById(101L)).thenReturn(Optional.of(otherTurno));

        assertFalse(evaluator.isTurnoDoctor(101L));
    }

    @Test
    void isDoctorAuthorCheck() {
        authenticate("doctor@esu.test", "MEDICO");
        when(usuarioRepositorio.findByEmail("doctor@esu.test")).thenReturn(Optional.of(doctorUser));
        when(profesionalRepositorio.findByUsuarioEmail("doctor@esu.test")).thenReturn(Optional.of(profesional));

        assertTrue(evaluator.isDoctorAuthor(20L));
        assertFalse(evaluator.isDoctorAuthor(999L));
    }
}
