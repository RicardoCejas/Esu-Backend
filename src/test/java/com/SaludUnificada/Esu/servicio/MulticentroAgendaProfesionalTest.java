package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.DisponibilidadProfesional;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.mapper.PacienteMapper;
import com.SaludUnificada.Esu.mapper.ProfesionalMapper;
import com.SaludUnificada.Esu.mapper.TurnoMapper;
import com.SaludUnificada.Esu.repositorio.*;
import com.SaludUnificada.Esu.seguridad.OwnershipSecurityEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MulticentroAgendaProfesionalTest {

    @Mock
    private TurnoRepositorio turnoRepositorio;
    @Mock
    private ProfesionalRepositorio profesionalRepositorio;
    @Mock
    private PacienteRepositorio pacienteRepositorio;
    @Mock
    private UsuarioRepositorio usuarioRepositorio;
    @Mock
    private DisponibilidadProfesionalRepositorio disponibilidadRepositorio;
    @Mock
    private RecordatorioTurnoRepositorio recordatorioRepositorio;
    @Mock
    private PacienteMapper pacienteMapper;
    @Mock
    private ProfesionalMapper profesionalMapper;

    private TurnoMapper turnoMapper;
    private TurnoServicio turnoServicio;
    private ProfesionalServicio profesionalServicio;
    private OwnershipSecurityEvaluator securityEvaluator;

    private Profesional profesional1;
    private Profesional profesional2;
    private Usuario doctorUser1;
    private Usuario doctorUser2;

    @BeforeEach
    void setUp() {
        turnoMapper = new TurnoMapper();
        ReflectionTestUtils.setField(turnoMapper, "pacienteMapper", pacienteMapper);
        ReflectionTestUtils.setField(turnoMapper, "profesionalMapper", profesionalMapper);

        Clock clock = Clock.fixed(LocalDateTime.of(2026, 10, 31, 10, 0).atZone(ZoneId.of("America/Argentina/Cordoba")).toInstant(), ZoneId.of("America/Argentina/Cordoba"));

        turnoServicio = new TurnoServicio(
                turnoRepositorio,
                turnoMapper,
                pacienteRepositorio,
                profesionalRepositorio,
                recordatorioRepositorio,
                clock,
                ZoneId.of("America/Argentina/Cordoba"),
                LocalTime.of(8, 0),
                LocalTime.of(20, 0),
                20
        );

        profesionalServicio = new ProfesionalServicio(profesionalRepositorio, profesionalMapper, turnoRepositorio);
        ReflectionTestUtils.setField(profesionalServicio, "usuarioRepositorio", usuarioRepositorio);

        securityEvaluator = new OwnershipSecurityEvaluator(
                usuarioRepositorio,
                pacienteRepositorio,
                disponibilidadRepositorio,
                profesionalRepositorio,
                turnoRepositorio,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        doctorUser1 = new Usuario();
        doctorUser1.setId(10L);
        doctorUser1.setEmail("doctor1@esu.test");
        doctorUser1.setRol("MEDICO");

        doctorUser2 = new Usuario();
        doctorUser2.setId(20L);
        doctorUser2.setEmail("doctor2@esu.test");
        doctorUser2.setRol("MEDICO");

        profesional1 = new Profesional();
        profesional1.setId(1L);
        profesional1.setNombre("Carlos");
        profesional1.setApellido("Martínez");
        profesional1.setMatricula("MP-1111");
        profesional1.setUsuario(doctorUser1);
        profesional1.setCentrosSaludIds(new HashSet<>(Set.of(1L, 2L)));

        profesional2 = new Profesional();
        profesional2.setId(2L);
        profesional2.setNombre("Lucía");
        profesional2.setApellido("González");
        profesional2.setMatricula("MP-2222");
        profesional2.setUsuario(doctorUser2);
        profesional2.setCentrosSaludIds(new HashSet<>(Set.of(1L)));
    }

    private void authenticate(String email, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, "pass", List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Un profesional puede consultar y gestionar múltiples centros de salud asignados")
    void testGestionMulticentroProfesional() {
        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(profesional1));

        Set<Long> centros = profesionalServicio.obtenerCentrosDelProfesional(1L);
        assertEquals(2, centros.size());
        assertTrue(centros.contains(1L));
        assertTrue(centros.contains(2L));

        when(profesionalRepositorio.save(any(Profesional.class))).thenReturn(profesional1);
        Set<Long> actualizados = profesionalServicio.vincularCentro(1L, 3L);
        assertTrue(actualizados.contains(3L));

        Set<Long> despuesDesvincular = profesionalServicio.desvincularCentro(1L, 2L);
        assertFalse(despuesDesvincular.contains(2L));
    }

    @Test
    @DisplayName("Debe consultar agenda filtrada por profesional, sede específica y fecha")
    void testObtenerAgendaProfesionalConSedeYFecha() {
        when(profesionalRepositorio.existsById(1L)).thenReturn(true);

        LocalDate fecha = LocalDate.of(2026, 11, 10);
        Turno turnoHospital = new Turno();
        turnoHospital.setId(101L);
        turnoHospital.setFechaHora(LocalDateTime.of(2026, 11, 10, 9, 0));
        turnoHospital.setEstado("CONFIRMADO");
        turnoHospital.setCentroSaludId(1L);
        turnoHospital.setCentroSaludNombre("Hospital Regional Aurelio Crespo");
        turnoHospital.setConsultorioNumero("101");
        turnoHospital.setProfesional(profesional1);

        when(turnoRepositorio.findAgendaProfesional(eq(1L), eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(turnoHospital));

        List<TurnoDtoResponse> agenda = turnoServicio.obtenerAgendaProfesional(1L, 1L, fecha);

        assertNotNull(agenda);
        assertEquals(1, agenda.size());
        assertEquals("Hospital Regional Aurelio Crespo", agenda.get(0).getCentroSaludNombre());
        assertEquals(1L, agenda.get(0).getCentroSaludId());
        assertEquals("101", agenda.get(0).getConsultorioNumero());
    }

    @Test
    @DisplayName("Debe consultar agenda del médico autenticado (mi-agenda) sin exponer turnos de otros")
    void testObtenerMiAgendaMedicoAutenticado() {
        when(profesionalRepositorio.findByUsuarioEmail("doctor1@esu.test")).thenReturn(Optional.of(profesional1));
        when(profesionalRepositorio.existsById(1L)).thenReturn(true);

        Turno turnoClinica = new Turno();
        turnoClinica.setId(102L);
        turnoClinica.setFechaHora(LocalDateTime.of(2026, 11, 10, 15, 0));
        turnoClinica.setEstado("CONFIRMADO");
        turnoClinica.setCentroSaludId(2L);
        turnoClinica.setCentroSaludNombre("Clínica Privada Cabrera");
        turnoClinica.setConsultorioNumero("Consultorio 2");
        turnoClinica.setProfesional(profesional1);

        when(turnoRepositorio.findAgendaProfesional(eq(1L), eq(2L), isNull(), isNull()))
                .thenReturn(List.of(turnoClinica));

        List<TurnoDtoResponse> miAgenda = turnoServicio.obtenerMiAgenda("doctor1@esu.test", 2L, null);

        assertNotNull(miAgenda);
        assertEquals(1, miAgenda.size());
        assertEquals(2L, miAgenda.get(0).getCentroSaludId());
        assertEquals("Clínica Privada Cabrera", miAgenda.get(0).getCentroSaludNombre());
    }

    @Test
    @DisplayName("La política de seguridad debe validar que un médico solo accede a su propia agenda y disponibilidad")
    void testSeguridadAutorizacionAgendaYDisponibilidad() {
        authenticate("doctor1@esu.test", "MEDICO");
        when(usuarioRepositorio.findByEmail("doctor1@esu.test")).thenReturn(Optional.of(doctorUser1));
        when(profesionalRepositorio.findByUsuarioEmail("doctor1@esu.test")).thenReturn(Optional.of(profesional1));

        assertTrue(securityEvaluator.isDoctorAuthor(1L));
        assertFalse(securityEvaluator.isDoctorAuthor(2L));

        DisponibilidadProfesional dispPropia = new DisponibilidadProfesional();
        dispPropia.setId(500L);
        dispPropia.setProfesional(profesional1);
        when(disponibilidadRepositorio.findById(500L)).thenReturn(Optional.of(dispPropia));

        assertTrue(securityEvaluator.canAccessDisponibilidad(500L));

        DisponibilidadProfesional dispAjena = new DisponibilidadProfesional();
        dispAjena.setId(501L);
        dispAjena.setProfesional(profesional2);
        when(disponibilidadRepositorio.findById(501L)).thenReturn(Optional.of(dispAjena));

        assertFalse(securityEvaluator.canAccessDisponibilidad(501L));
    }
}
