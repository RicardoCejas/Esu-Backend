package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.config.EsuInitialDataSeeder;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoPublicoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrganizacionCentrosServiciosTest {

    @Autowired
    private TurnoServicio turnoServicio;

    @Autowired
    private CentroSaludServicio centroSaludServicio;

    @Autowired
    private CatalogoEspecialidadesNormalizador catalogoNormalizador;

    @Autowired
    private CentroSaludRepositorio centroSaludRepositorio;

    @Autowired
    private EspecialidadRepositorio especialidadRepositorio;

    @Autowired
    private ProfesionalRepositorio profesionalRepositorio;

    @Autowired
    private PacienteRepositorio pacienteRepositorio;

    @Autowired
    private DisponibilidadProfesionalRepositorio disponibilidadRepositorio;

    @Autowired
    private TurnoRepositorio turnoRepositorio;

    @Autowired
    private EsuInitialDataSeeder initialDataSeeder;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    private CentroSalud hospitalCrespo;
    private CentroSalud clinicaCabrera;
    private CentroSalud centroDiagnostico;
    private CentroSalud sanatorioMariaIsabel;
    private CentroSalud dispensarioInactivo;

    private Especialidad espClinicaMedica;
    private Especialidad espCardiologia;
    private Profesional drMulticentro;
    private Paciente pacienteTest;

    @BeforeEach
    void setUp() {
        // Inicializar los 4 centros de salud según especificación de Cruz del Eje
        hospitalCrespo = centroSaludRepositorio.findById(1L).orElseGet(() -> {
            CentroSalud c = new CentroSalud();
            c.setId(1L);
            c.setNombre("Hospital Provincial Dr. Aurelio Crespo");
            c.setTipo("HOSPITAL_PUBLICO");
            c.setDireccion("Sarmiento 1250");
            c.setActivo(true);
            c.setPermiteConsultas(true);
            c.setPermiteEstudios(true);
            return centroSaludRepositorio.save(c);
        });

        clinicaCabrera = centroSaludRepositorio.findById(2L).orElseGet(() -> {
            CentroSalud c = new CentroSalud();
            c.setId(2L);
            c.setNombre("Clínica Cabrera");
            c.setTipo("CLINICA_PRIVADA");
            c.setDireccion("San Martín 450");
            c.setActivo(true);
            c.setPermiteConsultas(true);
            c.setPermiteEstudios(true);
            return centroSaludRepositorio.save(c);
        });

        centroDiagnostico = centroSaludRepositorio.findById(3L).orElseGet(() -> {
            CentroSalud c = new CentroSalud();
            c.setId(3L);
            c.setNombre("Centro de Diagnóstico por Imágenes Cruz del Eje");
            c.setTipo("CENTRO_DIAGNOSTICO");
            c.setDireccion("Mitre 820");
            c.setActivo(true);
            c.setPermiteConsultas(false); // SOLO ESTUDIOS
            c.setPermiteEstudios(true);
            return centroSaludRepositorio.save(c);
        });

        sanatorioMariaIsabel = centroSaludRepositorio.findById(5L).orElseGet(() -> {
            CentroSalud c = new CentroSalud();
            c.setId(5L);
            c.setNombre("Sanatorio María Isabel");
            c.setTipo("SANATORIO_PRIVADO");
            c.setDireccion("Pte. Perón 320");
            c.setActivo(true);
            c.setPermiteConsultas(true); // SOLO CONSULTAS
            c.setPermiteEstudios(false);
            return centroSaludRepositorio.save(c);
        });

        dispensarioInactivo = centroSaludRepositorio.findById(4L).orElseGet(() -> {
            CentroSalud c = new CentroSalud();
            c.setId(4L);
            c.setNombre("Dispensario Municipal Santa Rosa");
            c.setTipo("DISPENSARIO");
            c.setDireccion("Santa Rosa 100");
            c.setActivo(false); // DESACTIVADO PARA NUEVAS RESERVAS
            c.setPermiteConsultas(false);
            c.setPermiteEstudios(false);
            return centroSaludRepositorio.save(c);
        });
        dispensarioInactivo.setActivo(false);
        dispensarioInactivo.setPermiteConsultas(false);
        dispensarioInactivo.setPermiteEstudios(false);
        dispensarioInactivo = centroSaludRepositorio.save(dispensarioInactivo);

        // Especialidades
        espClinicaMedica = especialidadRepositorio.findByNombreIgnoreCase("Clínica Médica").orElseGet(() -> {
            Especialidad e = new Especialidad();
            e.setNombre("Clínica Médica");
            e.setNombreCanonico("Clínica Médica");
            e.setCategoria("CONSULTA");
            return especialidadRepositorio.save(e);
        });

        espCardiologia = especialidadRepositorio.findByNombreIgnoreCase("Cardiología").orElseGet(() -> {
            Especialidad e = new Especialidad();
            e.setNombre("Cardiología");
            e.setNombreCanonico("Cardiología");
            e.setCategoria("CONSULTA");
            return especialidadRepositorio.save(e);
        });

        Usuario uDoc = usuarioRepositorio.findByEmail("esteban.quiros@salud.gob.ar").orElseGet(() -> {
            Usuario u = new Usuario();
            u.setEmail("esteban.quiros@salud.gob.ar");
            u.setPassword(passwordEncoder.encode("Doctor123!"));
            u.setRol("PROFESIONAL");
            u.setEstadoActivo(true);
            u.setEmailVerificado(true);
            u.setFechaDeAlta(LocalDate.now());
            return usuarioRepositorio.save(u);
        });

        // Profesional multicentro (atiende en Hospital Crespo y Clínica Cabrera con horarios separados)
        drMulticentro = profesionalRepositorio.findByMatricula("MP-TEST-MULTI").orElseGet(() -> {
            Profesional p = new Profesional();
            p.setNombre("Esteban");
            p.setApellido("Quirós");
            p.setMatricula("MP-TEST-MULTI");
            p.setTelefono("3549-400111");
            p.setEstadoActivo(true);
            p.setUsuario(uDoc);
            p.setEspecialidades(Set.of(espClinicaMedica));
            p.setCentrosSaludIds(Set.of(1L, 2L));
            return profesionalRepositorio.save(p);
        });

        // Disponibilidad: Lunes en Hospital (08:00 - 12:00) y Martes en Clínica Cabrera (14:00 - 18:00)
        disponibilidadRepositorio.deleteAll();
        disponibilidadRepositorio.save(DisponibilidadProfesional.builder()
                .profesional(drMulticentro)
                .centroSaludId(1L)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .intervaloMinutos(30)
                .activo(true)
                .build());

        disponibilidadRepositorio.save(DisponibilidadProfesional.builder()
                .profesional(drMulticentro)
                .centroSaludId(2L)
                .diaSemana(DayOfWeek.TUESDAY)
                .horaInicio(LocalTime.of(14, 0))
                .horaFin(LocalTime.of(18, 0))
                .intervaloMinutos(30)
                .activo(true)
                .build());

        Usuario uPac = usuarioRepositorio.findByEmail("mario.oliva@salud.gob.ar").orElseGet(() -> {
            Usuario u = new Usuario();
            u.setEmail("mario.oliva@salud.gob.ar");
            u.setPassword(passwordEncoder.encode("Paciente123!"));
            u.setRol("PACIENTE");
            u.setEstadoActivo(true);
            u.setEmailVerificado(true);
            u.setFechaDeAlta(LocalDate.now());
            return usuarioRepositorio.save(u);
        });

        // Paciente
        pacienteTest = pacienteRepositorio.findByDni("30111222").orElseGet(() -> {
            Paciente pac = new Paciente();
            pac.setNombre("Mario");
            pac.setApellido("Oliva");
            pac.setDni("30111222");
            pac.setNroAfiliado("AF-30111222");
            pac.setObraSocial("Apross");
            pac.setTelefono("3549-429988");
            pac.setUsuario(uPac);
            pac.setCentroSaludReferenciaId(1L);
            pac.setCentroSaludReferenciaNombre("Hospital Provincial Dr. Aurelio Crespo");
            return pacienteRepositorio.save(pac);
        });
    }

    @Test
    @DisplayName("Escenario 1: Hospital permite reservar Consulta médica con profesional")
    @WithMockUser(username = "recepcion@esu.gob.ar", roles = {"RECEPCIONISTA"})
    void testHospitalPermiteConsultaMedica() {
        LocalDateTime proximoLunes = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY).atTime(9, 0);

        TurnoDtoRequest request = new TurnoDtoRequest();
        request.setPacienteId(pacienteTest.getId());
        request.setProfesionalId(drMulticentro.getId());
        request.setCentroSaludId(1L);
        request.setTipoAtencion("CONSULTA");
        request.setServicio("Clínica Médica");
        request.setEspecialidadId(espClinicaMedica.getId());
        request.setFechaHora(proximoLunes);
        request.setMotivoTurno("Consulta médica preventiva");

        TurnoDtoResponse response = turnoServicio.crearTurno(request, "recepcion@esu.gob.ar");

        assertNotNull(response);
        assertEquals(1L, response.getCentroSaludId());
        assertEquals("CONSULTA", response.getTipoAtencion());
        assertEquals("Clínica Médica", response.getServicio());
        assertEquals("Hospital Provincial Dr. Aurelio Crespo", response.getCentroSaludNombre());
    }

    @Test
    @DisplayName("Escenario 2: Hospital permite reservar Estudio diagnóstico sin requerir médico de consultorio")
    @WithMockUser(username = "recepcion@esu.gob.ar", roles = {"RECEPCIONISTA"})
    void testHospitalPermiteEstudioDiagnostico() {
        LocalDateTime fecha = LocalDate.now().plusDays(2).atTime(10, 0);

        TurnoPublicoDtoRequest request = new TurnoPublicoDtoRequest();
        request.setDni(pacienteTest.getDni());
        request.setNombre(pacienteTest.getNombre());
        request.setApellido(pacienteTest.getApellido());
        request.setTelefono(pacienteTest.getTelefono());
        request.setCentroSaludId(1L);
        request.setTipoAtencion("ESTUDIO");
        request.setServicio("Laboratorio");
        request.setFechaHora(fecha);
        request.setMotivoTurno("Análisis de sangre completo");

        TurnoDtoResponse response = turnoServicio.reservarTurnoPublico(request, pacienteTest.getUsuario().getEmail());

        assertNotNull(response);
        assertEquals("ESTUDIO", response.getTipoAtencion());
        assertEquals("Laboratorio", response.getServicio());
        assertEquals(1L, response.getCentroSaludId());
        assertNull(response.getProfesional());
    }

    @Test
    @DisplayName("Escenario 3: Clínica Cabrera ofrece tanto Consultas como Estudios válidos")
    void testClinicaCabreraCapacidades() {
        var resp = centroSaludServicio.obtenerPorId(clinicaCabrera.getId());
        assertTrue(resp.getPermiteConsultas());
        assertTrue(resp.getPermiteEstudios());
        assertTrue(resp.getEstudiosDisponibles().stream().anyMatch(e -> e.contains("Ecografía")));
        assertTrue(resp.getEstudiosDisponibles().stream().anyMatch(e -> e.contains("Radiografía")));
    }

    @Test
    @DisplayName("Escenario 4: Sanatorio María Isabel permite Consultas pero rechaza terminantemente Estudios")
    void testSanatorioMariaIsabelRechazaEstudios() {
        // Validación en servicio de turnero
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                turnoServicio.validarCombinacionCentroYCategoria(5L, "ESTUDIO", "Laboratorio", null, null)
        );
        assertTrue(ex.getMessage().contains("no ofrece estudios ni prácticas diagnósticas"));
    }

    @Test
    @DisplayName("Escenario 5: Centro de Diagnóstico rechaza Consultas de consultorio y ofrece Estudios")
    void testCentroDiagnosticoRechazaConsultas() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                turnoServicio.validarCombinacionCentroYCategoria(3L, "CONSULTA", null, espClinicaMedica.getId(), drMulticentro.getId())
        );
        assertTrue(ex.getMessage().contains("no ofrece consultas médicas de consultorio"));

        // En cambio, estudio válido es permitido
        assertDoesNotThrow(() ->
                turnoServicio.validarCombinacionCentroYCategoria(3L, "ESTUDIO", "Ecografía Mamaria", null, null)
        );
    }

    @Test
    @DisplayName("Escenario 6: Profesional multicentro conserva identidad única y horarios separados sin colisión")
    void testProfesionalMulticentroHorariosSeparados() {
        assertEquals(2, drMulticentro.getCentrosSaludIds().size());
        assertTrue(drMulticentro.getCentrosSaludIds().contains(1L));
        assertTrue(drMulticentro.getCentrosSaludIds().contains(2L));
        assertFalse(drMulticentro.getCentrosSaludIds().contains(3L), "Nunca asignar médico a Centro de Diagnóstico");

        List<DisponibilidadProfesional> disps = disponibilidadRepositorio.findByProfesionalIdAndActivoTrue(drMulticentro.getId());
        assertEquals(2, disps.size());
        // Lunes en centro 1, Martes en centro 2 -> Sin solapamiento
        assertNotEquals(disps.get(0).getDiaSemana(), disps.get(1).getDiaSemana());
    }

    @Test
    @DisplayName("Escenario 7: Agenda del profesional puede filtrarse por centro o consolidar todos")
    @WithMockUser(username = "recepcion@esu.gob.ar", roles = {"RECEPCIONISTA"})
    void testFiltrarAgendaPorCentro() {
        LocalDateTime tHosp = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY).atTime(9, 0);
        LocalDateTime tCabrera = LocalDate.now().plusWeeks(1).with(DayOfWeek.TUESDAY).atTime(15, 0);

        Turno t1 = new Turno();
        t1.setPaciente(pacienteTest);
        t1.setProfesional(drMulticentro);
        t1.setCentroSaludId(1L);
        t1.setFechaHora(tHosp);
        t1.setEstado("CONFIRMADO");
        turnoRepositorio.save(t1);

        Turno t2 = new Turno();
        t2.setPaciente(pacienteTest);
        t2.setProfesional(drMulticentro);
        t2.setCentroSaludId(2L);
        t2.setFechaHora(tCabrera);
        t2.setEstado("CONFIRMADO");
        turnoRepositorio.save(t2);

        // Agenda filtrada por Hospital (1L)
        List<TurnoDtoResponse> agendaHosp = turnoServicio.obtenerAgendaProfesional(drMulticentro.getId(), 1L, null);
        assertEquals(1, agendaHosp.size());
        assertEquals(1L, agendaHosp.get(0).getCentroSaludId());

        // Agenda filtrada por Cabrera (2L)
        List<TurnoDtoResponse> agendaCabrera = turnoServicio.obtenerAgendaProfesional(drMulticentro.getId(), 2L, null);
        assertEquals(1, agendaCabrera.size());
        assertEquals(2L, agendaCabrera.get(0).getCentroSaludId());

        // Agenda completa (sin filtro de centro)
        List<TurnoDtoResponse> agendaGlobal = turnoServicio.obtenerAgendaProfesional(drMulticentro.getId(), null, null);
        assertTrue(agendaGlobal.size() >= 2);
    }

    @Test
    @DisplayName("Escenario 8: Paciente tiene centro de referencia claro y turnos muestran centro de atención")
    void testPacienteReferenciaYAtencion() {
        assertEquals(1L, pacienteTest.getCentroSaludReferenciaId());
        assertEquals("Hospital Provincial Dr. Aurelio Crespo", pacienteTest.getCentroSaludReferenciaNombre());
    }

    @Test
    @DisplayName("Escenario 9: Catálogo normalizador unifica sinónimos y distingue modalidades de estudios")
    void testNormalizadorEspecialidadesYEstudios() {
        assertEquals("Clínica Médica", catalogoNormalizador.normalizarNombre("Clínica Medica"));
        assertEquals("Cirugía General", catalogoNormalizador.normalizarNombre("Cirugía"));
        assertEquals("Ginecología y Obstetricia", catalogoNormalizador.normalizarNombre("Toco ginecología"));
        assertEquals("Psicología", catalogoNormalizador.normalizarNombre("Psicología Adultos y Niños"));

        assertTrue(catalogoNormalizador.esModalidadEstudio("Laboratorio"));
        assertTrue(catalogoNormalizador.esModalidadEstudio("Ecografía General"));
        assertTrue(catalogoNormalizador.esModalidadEstudio("Radiografía"));
        assertFalse(catalogoNormalizador.esModalidadEstudio("Pediatría"));
    }

    @Test
    @DisplayName("Escenario 10: Ejecución idempotente del Seeder inicial sin duplicaciones")
    void testSeederIdempotente() {
        assertDoesNotThrow(() -> initialDataSeeder.run());
        assertDoesNotThrow(() -> initialDataSeeder.run());

        // Verificar que los centros activos siguen siendo 4
        List<CentroSalud> activos = centroSaludRepositorio.findByActivoTrueOrderByIdAsc();
        assertEquals(4, activos.size());
    }

    @Test
    @DisplayName("Escenario 11: Rechazo de centro de salud inactivo para nuevas reservas")
    void testRechazoCentroInactivo() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                turnoServicio.validarCombinacionCentroYCategoria(dispensarioInactivo.getId(), "CONSULTA", null, espClinicaMedica.getId(), drMulticentro.getId())
        );
        assertTrue(ex.getMessage().contains("inactivo para nuevas reservas"));
    }
}
