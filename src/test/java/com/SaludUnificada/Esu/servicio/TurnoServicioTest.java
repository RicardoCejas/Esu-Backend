package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.AtencionTurnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.CopagoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.mapper.TurnoMapper;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.RecordatorioTurnoRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class TurnoServicioTest {
    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Cordoba");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T13:00:00Z"), ZONE);

    @Mock private TurnoRepositorio turnos;
    @Mock private TurnoMapper mapper;
    @Mock private PacienteRepositorio pacientes;
    @Mock private ProfesionalRepositorio profesionales;
    @Mock private RecordatorioTurnoRepositorio recordatorios;
    private TurnoServicio servicio;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        servicio = new TurnoServicio(turnos, mapper, pacientes, profesionales, recordatorios, CLOCK, ZONE,
                java.time.LocalTime.of(8, 0), java.time.LocalTime.of(17, 0), 30);
    }

    @Test
    void disponibilidadSoloIncluyeSlotsFuturosYNoOcupados() {
        LocalDate fecha = LocalDate.of(2026, 10, 14);
        when(profesionales.existsById(12L)).thenReturn(true);
        Turno ocupado = new Turno();
        ocupado.setFechaHora(fecha.atTime(11, 0));
        when(turnos.findByProfesionalIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
                12L, fecha.atTime(8, 0), fecha.atTime(17, 0), "CANCELADO")).thenReturn(List.of(ocupado));

        List<LocalDateTime> disponibles = servicio.obtenerHorariosDisponibles(12L, fecha);

        assertFalse(disponibles.contains(fecha.atTime(10, 0)));
        assertFalse(disponibles.contains(fecha.atTime(11, 0)));
        assertTrue(disponibles.contains(fecha.atTime(10, 30)));
        assertTrue(disponibles.contains(fecha.atTime(11, 30)));
        assertEquals(fecha.atTime(16, 30), disponibles.get(disponibles.size() - 1));
    }

    @Test
    void fechaNoLaborableNoOfreceSlots() {
        when(profesionales.existsById(12L)).thenReturn(true);
        assertTrue(servicio.obtenerHorariosDisponibles(12L, LocalDate.of(2026, 10, 17)).isEmpty());
        verify(turnos, never()).findByProfesionalIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
                any(), any(), any(), any());
    }

    @Test
    void disponibilidadRespetaLaJornadaYDuracionConfiguradas() {
        TurnoServicio configurado = new TurnoServicio(turnos, mapper, pacientes, profesionales, recordatorios,
                CLOCK, ZONE, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(11, 0), 60);
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        when(profesionales.existsById(12L)).thenReturn(true);
        when(turnos.findByProfesionalIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
                12L, fecha.atTime(9, 0), fecha.atTime(11, 0), "CANCELADO")).thenReturn(List.of());

        var slots = configurado.obtenerHorariosDisponibles(12L, fecha);

        assertEquals(List.of(fecha.atTime(9, 0), fecha.atTime(10, 0)), slots);
    }

    @Test
    void crearTurnoTomaPacienteDelEmailAutenticadoYGuardaRecordatorio() {
        var paciente = new Paciente(); paciente.setId(21L);
        var profesional = new Profesional(); profesional.setId(12L);
        var request = new TurnoDtoRequest(LocalDateTime.of(2026, 10, 15, 9, 30), 12L, "Consulta");
        when(profesionales.findByIdForUpdate(12L)).thenReturn(Optional.of(profesional));
        when(turnos.existsByProfesionalIdAndFechaHoraAndEstadoNot(12L, request.getFechaHora(), "CANCELADO")).thenReturn(false);
        when(pacientes.findByUsuarioEmail("patient@example.test")).thenReturn(Optional.of(paciente));
        when(turnos.save(any(Turno.class))).thenAnswer(invocation -> {
            Turno saved = invocation.getArgument(0); saved.setId(55L); return saved;
        });
        when(mapper.paraDto(any(Turno.class))).thenReturn(null);

        servicio.crearTurno(request, "patient@example.test");

        verify(turnos).save(argThat(t -> t.getPaciente() == paciente && t.getProfesional() == profesional));
        verify(recordatorios).save(argThat(r -> r.getTurno().getId().equals(55L)
                && r.getDisponibleDesde().equals(request.getFechaHora().minusHours(24))));
        verify(pacientes).findByUsuarioEmail("patient@example.test");
    }

    @Test
    void crearTurnoRechazaSlotOcupadoConConflicto() {
        var profesional = new Profesional(); profesional.setId(12L);
        var request = new TurnoDtoRequest(LocalDateTime.of(2026, 10, 15, 9, 30), 12L, null);
        when(profesionales.findByIdForUpdate(12L)).thenReturn(Optional.of(profesional));
        when(turnos.existsByProfesionalIdAndFechaHoraAndEstadoNot(12L, request.getFechaHora(), "CANCELADO")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> servicio.crearTurno(request, "patient@example.test"));
        verify(pacientes, never()).findByUsuarioEmail(any());
        verify(turnos, never()).save(any());
    }

    @Test
    void crearTurnoRechazaSabados() {
        var request = new TurnoDtoRequest(LocalDateTime.of(2026, 10, 17, 9, 30), 12L, null);
        assertThrows(IllegalArgumentException.class, () -> servicio.crearTurno(request, "patient@example.test"));
        verify(profesionales, never()).findByIdForUpdate(any());
    }

    @Test
    void realizarCheckInCambiaEstadoAEnSalaDeEsperaYGuardaRecepcionista() {
        Turno turno = new Turno();
        turno.setId(10L);
        turno.setEstado("PENDIENTE");

        when(turnos.findById(10L)).thenReturn(Optional.of(turno));
        when(turnos.save(any(Turno.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.paraDto(any(Turno.class))).thenAnswer(inv -> {
            Turno t = inv.getArgument(0);
            return TurnoDtoResponse.builder()
                    .id(t.getId())
                    .estado(t.getEstado())
                    .fechaCheckIn(t.getFechaCheckIn())
                    .recepcionistaCheckIn(t.getRecepcionistaCheckIn())
                    .build();
        });

        TurnoDtoResponse response = servicio.realizarCheckIn(10L, "recepcion@clinica.com");

        assertNotNull(response);
        assertEquals("EN_SALA_DE_ESPERA", response.getEstado());
        assertEquals("recepcion@clinica.com", response.getRecepcionistaCheckIn());
        assertNotNull(response.getFechaCheckIn());
    }

    @Test
    void realizarCheckInRechazaTurnoCancelado() {
        Turno turno = new Turno();
        turno.setId(11L);
        turno.setEstado("CANCELADO");

        when(turnos.findById(11L)).thenReturn(Optional.of(turno));

        assertThrows(IllegalStateException.class, () -> servicio.realizarCheckIn(11L, "recepcion@clinica.com"));
        verify(turnos, never()).save(any());
    }

    @Test
    void registrarCopagoGuardaDatosFinancieros() {
        Turno turno = new Turno();
        turno.setId(20L);
        turno.setEstado("CONFIRMADO");

        CopagoDtoRequest copago = CopagoDtoRequest.builder()
                .monto(new BigDecimal("2500.50"))
                .numeroBono("BONO-7788")
                .metodoPago("EFECTIVO")
                .build();

        when(turnos.findById(20L)).thenReturn(Optional.of(turno));
        when(turnos.save(any(Turno.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.paraDto(any(Turno.class))).thenAnswer(inv -> {
            Turno t = inv.getArgument(0);
            return TurnoDtoResponse.builder()
                    .id(t.getId())
                    .montoCopago(t.getMontoCopago())
                    .numeroBono(t.getNumeroBono())
                    .metodoPago(t.getMetodoPago())
                    .build();
        });

        TurnoDtoResponse response = servicio.registrarCopago(20L, copago);

        assertNotNull(response);
        assertEquals(new BigDecimal("2500.50"), response.getMontoCopago());
        assertEquals("BONO-7788", response.getNumeroBono());
        assertEquals("EFECTIVO", response.getMetodoPago());
    }

    @Test
    void atenderTurnoTransicionaAAtendidoYRegistraObservaciones() {
        Turno turno = new Turno();
        turno.setId(30L);
        turno.setEstado("EN_SALA_DE_ESPERA");
        turno.setPaciente(new Paciente());
        Profesional prof = new Profesional();
        Usuario u = new Usuario();
        u.setEmail("medico@clinica.com");
        prof.setUsuario(u);
        turno.setProfesional(prof);

        AtencionTurnoDtoRequest atencion = AtencionTurnoDtoRequest.builder()
                .observaciones("Paciente presenta mejoría sintomática. Tratamiento completado con éxito.")
                .build();

        when(turnos.findById(30L)).thenReturn(Optional.of(turno));
        when(turnos.save(any(Turno.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.paraDto(any(Turno.class))).thenAnswer(inv -> {
            Turno t = inv.getArgument(0);
            return TurnoDtoResponse.builder()
                    .id(t.getId())
                    .estado(t.getEstado())
                    .observacionesAtencion(t.getObservacionesAtencion())
                    .fechaAtencion(t.getFechaAtencion())
                    .build();
        });

        TurnoDtoResponse response = servicio.atenderTurno(30L, atencion, "medico@clinica.com");

        assertNotNull(response);
        assertEquals("ATENDIDO", response.getEstado());
        assertEquals("Paciente presenta mejoría sintomática. Tratamiento completado con éxito.", response.getObservacionesAtencion());
        assertNotNull(response.getFechaAtencion());
    }
    @Test
    void testListarPaginados() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(turnos.findAll(pageable)).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(new Turno())));
        when(mapper.paraDto(any())).thenReturn(new TurnoDtoResponse());

        org.springframework.data.domain.Page<TurnoDtoResponse> page = servicio.listarPaginados(pageable);
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void marcarAusenteFallaSiHorarioEsFuturo() {
        Turno turno = new Turno();
        turno.setId(77L);
        turno.setEstado("CONFIRMADO");
        turno.setFechaHora(LocalDateTime.of(2026, 10, 20, 10, 0)); // Futuro respecto a CLOCK (14/10)

        when(turnos.findById(77L)).thenReturn(Optional.of(turno));

        assertThrows(com.SaludUnificada.Esu.error.ReglaNegocioException.class, () -> {
            servicio.marcarAusente(77L, "recepcion@clinica.com");
        });
    }

    @Test
    void cancelarTurnoEliminaRecordatorio() {
        Turno turno = new Turno();
        turno.setId(88L);
        turno.setEstado("CONFIRMADO");
        turno.setFechaHora(LocalDateTime.of(2026, 10, 15, 12, 0)); // Más de 2 horas en futuro

        when(turnos.findById(88L)).thenReturn(Optional.of(turno));
        when(turnos.save(any(Turno.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.paraDto(any())).thenReturn(new TurnoDtoResponse());

        servicio.cancelarTurno(88L, "admin@clinica.com");

        assertEquals("CANCELADO", turno.getEstado());
        verify(recordatorios).deleteByTurnoId(88L);
    }
}
