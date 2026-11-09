package com.SaludUnificada.Esu.seguridad;

import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
    @Mock
    private EvolucionMedicaRepositorio evolucionMedicaRepositorio;
    @Mock
    private VinculoFamiliarRepositorio vinculoFamiliarRepositorio;

    @InjectMocks
    private OwnershipSecurityEvaluator evaluator;

    private Usuario adminUser;
    private Usuario doctorUser;
    private Usuario patientUser;
    private Paciente paciente;
    private Profesional profesional;

        @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
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
        patientUser.setEmail("paciente@esu.test");
        patientUser.setRol("PACIENTE");

        paciente = new Paciente();
        paciente.setId(10L);
        paciente.setUsuario(patientUser);

        profesional = new Profesional();
        profesional.setId(20L);
        profesional.setUsuario(doctorUser);
        profesional.setEstadoActivo(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String email, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, "pass", List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void adminCannotAccessPatientClinically() {
        authenticate("admin@esu.test", "ADMIN");
        assertFalse(evaluator.canAccessPatient(999L));
    }

    @Test
    void adminCanAccessPatientAdmin() {
        authenticate("admin@esu.test", "ADMIN");
        assertTrue(evaluator.canAccessPatientAdmin(10L));
    }

    @Test
    void patientCanOnlyAccessOwnData() {
        authenticate("paciente@esu.test", "PACIENTE");
        when(usuarioRepositorio.findByEmail("paciente@esu.test")).thenReturn(Optional.of(patientUser));
        when(pacienteRepositorio.findByUsuarioEmail("paciente@esu.test")).thenReturn(Optional.of(paciente));

        assertTrue(evaluator.canAccessPatient(10L));
        assertTrue(evaluator.isCurrentPatient(10L));

        // AISLAMIENTO: No debe permitir acceso si el ID consultado coincide con Usuario.id (3L)
        assertFalse(evaluator.canAccessPatient(3L));
        assertFalse(evaluator.isCurrentPatient(3L));

        // No debe permitir acceso a otro paciente (99L)
        assertFalse(evaluator.canAccessPatient(99L));
        assertFalse(evaluator.isCurrentPatient(99L));
    }

    @Test
    void receptionistHasNoClinicalAccessToPatient() {
        authenticate("recepcion@esu.test", "RECEPCIONISTA");
        when(usuarioRepositorio.findByEmail("recepcion@esu.test")).thenReturn(Optional.empty());

        assertFalse(evaluator.canAccessPatient(10L));
    }

    @Test
    void receptionistHasAdminAccessToPatient() {
        authenticate("recepcion@esu.test", "RECEPCIONISTA");
        assertTrue(evaluator.canAccessPatientAdmin(10L));
    }

    @Test
    void doctorWithoutCareRelationshipCannotAccessPatient() {
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
    void doctorWithPreviousTurnoCanAccessPatient() {
        authenticate("doctor@esu.test", "MEDICO");
        when(usuarioRepositorio.findByEmail("doctor@esu.test")).thenReturn(Optional.of(doctorUser));
        when(profesionalRepositorio.findByUsuarioEmail("doctor@esu.test")).thenReturn(Optional.of(profesional));
        when(turnoRepositorio.existsByProfesionalIdAndPacienteId(20L, 10L)).thenReturn(true);

        assertTrue(evaluator.canAccessPatient(10L));
    }

    @Test
    void doctorWithActiveEmergencyAccessCanAccessPatient() {
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

    @Test
    void canAccessEvolucionCheck() {
        authenticate("admin@esu.test", "ADMIN");
        assertFalse(evaluator.canAccessEvolucion(50L));

        authenticate("paciente@esu.test", "PACIENTE");
        EvolucionMedica ev = new EvolucionMedica();
        ev.setId(50L);
        ev.setPaciente(paciente);
        when(evolucionMedicaRepositorio.findById(50L)).thenReturn(Optional.of(ev));
        when(usuarioRepositorio.findByEmail("paciente@esu.test")).thenReturn(Optional.of(patientUser));
        when(pacienteRepositorio.findByUsuarioEmail("paciente@esu.test")).thenReturn(Optional.of(paciente));

        assertTrue(evaluator.canAccessEvolucion(50L));
    }

    @Test
    void canAccessRecetaCheck() {
        RecetaDigital receta = new RecetaDigital();
        receta.setId(300L);
        receta.setPaciente(paciente);
        receta.setProfesional(profesional);

        when(recetaRepositorio.findById(300L)).thenReturn(Optional.of(receta));

        // 1. Admin
        authenticate("admin@esu.test", "ADMIN");
        assertFalse(evaluator.canAccessReceta(300L));

        // 2. Farmacia
        authenticate("farmacia@esu.test", "FARMACIA");
        assertTrue(evaluator.canAccessReceta(300L));

        // 3. Paciente titular
        authenticate("paciente@esu.test", "PACIENTE");
        when(usuarioRepositorio.findByEmail("paciente@esu.test")).thenReturn(Optional.of(patientUser));
        assertTrue(evaluator.canAccessReceta(300L));

        // 4. Médico autor
        authenticate("doctor@esu.test", "MEDICO");
        when(usuarioRepositorio.findByEmail("doctor@esu.test")).thenReturn(Optional.of(doctorUser));
        assertTrue(evaluator.canAccessReceta(300L));

        // 5. Otro paciente no autorizado
        Usuario otroUsuario = new Usuario();
        otroUsuario.setId(88L);
        otroUsuario.setEmail("otro@esu.test");
        otroUsuario.setRol("PACIENTE");
        authenticate("otro@esu.test", "PACIENTE");
        when(usuarioRepositorio.findByEmail("otro@esu.test")).thenReturn(Optional.of(otroUsuario));
        assertFalse(evaluator.canAccessReceta(300L));
    }

    @Test
    void canAccessEstudioCheck() {
        EstudioAdjunto estudio = new EstudioAdjunto();
        estudio.setId(400L);
        estudio.setPacienteId(10L);

        when(estudioRepositorio.findById(400L)).thenReturn(Optional.of(estudio));

        // 1. Admin
        authenticate("admin@esu.test", "ADMIN");
        assertFalse(evaluator.canAccessEstudio(400L));

        // 2. Paciente dueño
        authenticate("paciente@esu.test", "PACIENTE");
        when(usuarioRepositorio.findByEmail("paciente@esu.test")).thenReturn(Optional.of(patientUser));
        when(pacienteRepositorio.findByUsuarioEmail("paciente@esu.test")).thenReturn(Optional.of(paciente));
        assertTrue(evaluator.canAccessEstudio(400L));

        // 3. Otro paciente no autorizado
        Usuario otroUsuario = new Usuario();
        otroUsuario.setId(88L);
        otroUsuario.setEmail("otro@esu.test");
        otroUsuario.setRol("PACIENTE");
        Paciente otroPaciente = new Paciente();
        otroPaciente.setId(88L);

        authenticate("otro@esu.test", "PACIENTE");
        when(usuarioRepositorio.findByEmail("otro@esu.test")).thenReturn(Optional.of(otroUsuario));
        when(pacienteRepositorio.findByUsuarioEmail("otro@esu.test")).thenReturn(Optional.of(otroPaciente));
        assertFalse(evaluator.canAccessEstudio(400L));
    }

    @Test
    void canAccessHistoriaClinicaCheck() {
        HistoriaClinica hc = new HistoriaClinica();
        hc.setId(500L);
        hc.setPaciente(paciente);

        when(historiaClinicaRepositorio.findById(500L)).thenReturn(Optional.of(hc));

        authenticate("admin@esu.test", "ADMIN");
        assertFalse(evaluator.canAccessHistoriaClinica(500L));

        authenticate("paciente@esu.test", "PACIENTE");
        when(usuarioRepositorio.findByEmail("paciente@esu.test")).thenReturn(Optional.of(patientUser));
        when(pacienteRepositorio.findByUsuarioEmail("paciente@esu.test")).thenReturn(Optional.of(paciente));
        assertTrue(evaluator.canAccessHistoriaClinica(500L));
    }
    @Test
    void receptionistCanOnlyAccessTurnoOfOwnCenter() {
        Turno turnoCentro1 = new Turno();
        turnoCentro1.setId(601L);
        turnoCentro1.setCentroSaludId(1L);

        when(turnoRepositorio.findById(601L)).thenReturn(Optional.of(turnoCentro1));

        Usuario recepCentro1 = new Usuario();
        recepCentro1.setId(101L);
        recepCentro1.setEmail("recep.crespo@esu.test");
        recepCentro1.setRol("RECEPCIONISTA");
        recepCentro1.setCentroSaludId(1L);

        authenticate("recep.crespo@esu.test", "RECEPCIONISTA");
        when(usuarioRepositorio.findByEmail("recep.crespo@esu.test")).thenReturn(Optional.of(recepCentro1));

        assertTrue(evaluator.canAccessTurno(601L), "Recepcionista del Centro 1 debe poder acceder al turno de su centro");
    }

    @Test
    void receptionistCannotAccessTurnoOfOtherCenter() {
        Turno turnoCentro2 = new Turno();
        turnoCentro2.setId(602L);
        turnoCentro2.setCentroSaludId(2L);

        when(turnoRepositorio.findById(602L)).thenReturn(Optional.of(turnoCentro2));

        Usuario recepCentro1 = new Usuario();
        recepCentro1.setId(101L);
        recepCentro1.setEmail("recep.crespo@esu.test");
        recepCentro1.setRol("RECEPCIONISTA");
        recepCentro1.setCentroSaludId(1L);

        authenticate("recep.crespo@esu.test", "RECEPCIONISTA");
        when(usuarioRepositorio.findByEmail("recep.crespo@esu.test")).thenReturn(Optional.of(recepCentro1));

        assertFalse(evaluator.canAccessTurno(602L), "Recepcionista del Centro 1 NO debe poder acceder al turno del Centro 2 (aislamiento multi-tenant)");
    }

    @Test
    void receptionistWithoutCenterIsDeniedAccess_failClosed() {
        Turno turnoCentro1 = new Turno();
        turnoCentro1.setId(603L);
        turnoCentro1.setCentroSaludId(1L);

        when(turnoRepositorio.findById(603L)).thenReturn(Optional.of(turnoCentro1));

        Usuario recepSinCentro = new Usuario();
        recepSinCentro.setId(102L);
        recepSinCentro.setEmail("recep.flotante@esu.test");
        recepSinCentro.setRol("RECEPCIONISTA");
        recepSinCentro.setCentroSaludId(null); // Sin centro asignado

        authenticate("recep.flotante@esu.test", "RECEPCIONISTA");
        when(usuarioRepositorio.findByEmail("recep.flotante@esu.test")).thenReturn(Optional.of(recepSinCentro));

        assertFalse(evaluator.canAccessTurno(603L), "Recepcionista sin centro asignado debe ser bloqueado fail-closed");
    }

    @Test
    void isReceptionistOfCenter_validatesInstitutionalBoundary() {
        Usuario recepCentro1 = new Usuario();
        recepCentro1.setId(101L);
        recepCentro1.setEmail("recep.crespo@esu.test");
        recepCentro1.setRol("RECEPCIONISTA");
        recepCentro1.setCentroSaludId(1L);

        authenticate("recep.crespo@esu.test", "RECEPCIONISTA");
        when(usuarioRepositorio.findByEmail("recep.crespo@esu.test")).thenReturn(Optional.of(recepCentro1));

        assertTrue(evaluator.isReceptionistOfCenter(1L));
        assertFalse(evaluator.isReceptionistOfCenter(2L));
        assertFalse(evaluator.isReceptionistOfCenter(null));
    }

    @Test
    void canManageTurnoMostrador_enforcesInstitutionalBoundary() {
        Turno turnoCentro1 = new Turno();
        turnoCentro1.setId(604L);
        turnoCentro1.setCentroSaludId(1L);

        Turno turnoCentro2 = new Turno();
        turnoCentro2.setId(605L);
        turnoCentro2.setCentroSaludId(2L);

        when(turnoRepositorio.findById(604L)).thenReturn(Optional.of(turnoCentro1));
        when(turnoRepositorio.findById(605L)).thenReturn(Optional.of(turnoCentro2));

        Usuario recepCentro1 = new Usuario();
        recepCentro1.setId(101L);
        recepCentro1.setEmail("recep.crespo@esu.test");
        recepCentro1.setRol("RECEPCIONISTA");
        recepCentro1.setCentroSaludId(1L);

        authenticate("recep.crespo@esu.test", "RECEPCIONISTA");
        when(usuarioRepositorio.findByEmail("recep.crespo@esu.test")).thenReturn(Optional.of(recepCentro1));

        assertTrue(evaluator.canManageTurnoMostrador(604L), "Recepcionista debe poder gestionar mostrador de su centro");
        assertFalse(evaluator.canManageTurnoMostrador(605L), "Recepcionista NO debe poder gestionar mostrador de otro centro");
    }
}
