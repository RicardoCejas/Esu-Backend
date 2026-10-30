package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.BloqueoAgendaDtoRequest;
import com.SaludUnificada.Esu.dto.response.BloqueoAgendaDtoResponse;
import com.SaludUnificada.Esu.entidad.BloqueoAgenda;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.BloqueoAgendaMapper;
import com.SaludUnificada.Esu.repositorio.BloqueoAgendaRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BloqueoAgendaServicioTest {

    @Mock
    private BloqueoAgendaRepositorio bloqueoAgendaRepositorio;

    @Mock
    private ProfesionalRepositorio profesionalRepositorio;

    @Mock
    private TurnoRepositorio turnoRepositorio;

    @Spy
    private BloqueoAgendaMapper bloqueoAgendaMapper;

    @InjectMocks
    private BloqueoAgendaServicio bloqueoAgendaServicio;

    private Profesional profesional;
    private Paciente paciente1;
    private Paciente paciente2;

    @BeforeEach
    void setUp() {
        profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Carlos");
        profesional.setApellido("Martínez");

        paciente1 = new Paciente();
        paciente1.setId(10L);
        paciente1.setNombre("Juan");
        paciente1.setApellido("Pérez");
        paciente1.setDni("30111222");
        paciente1.setTelefono("03549-15441122");

        paciente2 = new Paciente();
        paciente2.setId(20L);
        paciente2.setNombre("Ana");
        paciente2.setApellido("López");
        paciente2.setDni("32333444");
        paciente2.setTelefono("03549-15443344");
    }

    @Test
    @DisplayName("Bloqueo total de jornada debe actualizar en lote todos los turnos y reportar pacientes a reprogramar")
    void testBloqueoTotalJornada_CancelaTodosLosTurnos() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(2).withHour(8).withMinute(0);
        LocalDateTime fin = LocalDateTime.now().plusDays(2).withHour(20).withMinute(0);

        BloqueoAgendaDtoRequest request = BloqueoAgendaDtoRequest.builder()
                .fechaInicio(inicio)
                .fechaFin(fin)
                .motivo("Congreso Médico Anual de Pediatría")
                .build();

        Turno turno1 = new Turno();
        turno1.setId(101L);
        turno1.setFechaHora(inicio.plusHours(1));
        turno1.setEstado("CONFIRMADO");
        turno1.setPaciente(paciente1);
        turno1.setProfesional(profesional);

        Turno turno2 = new Turno();
        turno2.setId(102L);
        turno2.setFechaHora(inicio.plusHours(3));
        turno2.setEstado("PENDIENTE");
        turno2.setPaciente(paciente2);
        turno2.setProfesional(profesional);

        Turno turnoVacio = new Turno();
        turnoVacio.setId(103L);
        turnoVacio.setFechaHora(inicio.plusHours(5));
        turnoVacio.setEstado("DISPONIBLE");
        turnoVacio.setPaciente(null);
        turnoVacio.setProfesional(profesional);

        List<Turno> turnosDelDia = new ArrayList<>(List.of(turno1, turno2, turnoVacio));

        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(profesional));
        when(bloqueoAgendaRepositorio.save(any(BloqueoAgenda.class))).thenAnswer(invocation -> {
            BloqueoAgenda b = invocation.getArgument(0);
            b.setId(50L);
            return b;
        });
        when(turnoRepositorio.findByProfesionalIdAndFechaHoraBetween(1L, inicio, fin)).thenReturn(turnosDelDia);

        BloqueoAgendaDtoResponse response = bloqueoAgendaServicio.registrarBloqueo(1L, request);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals(1L, response.getProfesionalId());
        // Solo reporta pacientes asignados para reprogramar (2)
        assertEquals(2, response.getTurnosAfectados());
        assertEquals(2, response.getTurnosCancelados().size());

        // Verificar que los estados cambiaron a CANCELADO_POR_MEDICO
        assertEquals("CANCELADO_POR_MEDICO", turno1.getEstado());
        assertEquals("CANCELADO_POR_MEDICO", turno2.getEstado());
        assertEquals("CANCELADO_POR_MEDICO", turnoVacio.getEstado());

        verify(turnoRepositorio).saveAll(turnosDelDia);
    }

    @Test
    @DisplayName("Bloqueo parcial de jornada debe cancelar únicamente los turnos comprendidos en la franja")
    void testBloqueoParcialJornada_SoloCancelaRangoSolicitado() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime fin = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);

        BloqueoAgendaDtoRequest request = BloqueoAgendaDtoRequest.builder()
                .fechaInicio(inicio)
                .fechaFin(fin)
                .motivo("Trámite personal / Turno bancario")
                .build();

        Turno turnoEnFranja = new Turno();
        turnoEnFranja.setId(201L);
        turnoEnFranja.setFechaHora(inicio.plusMinutes(30));
        turnoEnFranja.setEstado("CONFIRMADO");
        turnoEnFranja.setPaciente(paciente1);
        turnoEnFranja.setProfesional(profesional);

        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(profesional));
        when(bloqueoAgendaRepositorio.save(any(BloqueoAgenda.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(turnoRepositorio.findByProfesionalIdAndFechaHoraBetween(1L, inicio, fin)).thenReturn(List.of(turnoEnFranja));

        BloqueoAgendaDtoResponse response = bloqueoAgendaServicio.registrarBloqueo(1L, request);

        assertNotNull(response);
        assertEquals(1, response.getTurnosAfectados());
        assertEquals("CANCELADO_POR_MEDICO", turnoEnFranja.getEstado());
        assertEquals("Juan Pérez", response.getTurnosCancelados().get(0).getNombrePaciente());
        verify(turnoRepositorio).saveAll(List.of(turnoEnFranja));
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException si fechaInicio no es anterior a fechaFin")
    void testRegistrarBloqueo_FechaInvalida() {
        LocalDateTime ahora = LocalDateTime.now().plusDays(1);
        BloqueoAgendaDtoRequest request = BloqueoAgendaDtoRequest.builder()
                .fechaInicio(ahora.plusHours(2))
                .fechaFin(ahora)
                .motivo("Error de fechas")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                bloqueoAgendaServicio.registrarBloqueo(1L, request));

        verify(bloqueoAgendaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar NoEncontradoExcepcion si el profesional no existe")
    void testRegistrarBloqueo_ProfesionalNoEncontrado() {
        when(profesionalRepositorio.findById(999L)).thenReturn(Optional.empty());

        BloqueoAgendaDtoRequest request = BloqueoAgendaDtoRequest.builder()
                .fechaInicio(LocalDateTime.now().plusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(1).plusHours(2))
                .motivo("Vacaciones")
                .build();

        assertThrows(NoEncontradoExcepcion.class, () ->
                bloqueoAgendaServicio.registrarBloqueo(999L, request));
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException si el bloqueo se intenta registrar en el pasado")
    void testRegistrarBloqueo_EnElPasado_LanzaIllegalArgumentException() {
        BloqueoAgendaDtoRequest request = BloqueoAgendaDtoRequest.builder()
                .fechaInicio(LocalDateTime.now().minusDays(2))
                .fechaFin(LocalDateTime.now().minusDays(1))
                .motivo("Bloqueo retroactivo inválido")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                bloqueoAgendaServicio.registrarBloqueo(1L, request));

        verify(bloqueoAgendaRepositorio, never()).save(any());
    }
}
