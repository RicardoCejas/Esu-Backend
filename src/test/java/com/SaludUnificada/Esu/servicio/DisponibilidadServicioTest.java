package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.DisponibilidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.DisponibilidadDtoResponse;
import com.SaludUnificada.Esu.entidad.BloqueoAgenda;
import com.SaludUnificada.Esu.entidad.DisponibilidadProfesional;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.DisponibilidadMapper;
import com.SaludUnificada.Esu.repositorio.BloqueoAgendaRepositorio;
import com.SaludUnificada.Esu.repositorio.DisponibilidadProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServicioTest {

    @Mock
    private DisponibilidadProfesionalRepositorio disponibilidadRepositorio;

    @Mock
    private BloqueoAgendaRepositorio bloqueoAgendaRepositorio;

    @Mock
    private TurnoRepositorio turnoRepositorio;

    @Mock
    private ProfesionalRepositorio profesionalRepositorio;

    @Spy
    private DisponibilidadMapper disponibilidadMapper;

    @Mock
    private FeriadoServicio feriadoServicio;

    @InjectMocks
    private DisponibilidadServicio disponibilidadServicio;

    private Profesional profesional;
    private DisponibilidadDtoRequest request;

    @BeforeEach
    void setUp() {
        profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Carlos");
        profesional.setApellido("Martínez");

        request = DisponibilidadDtoRequest.builder()
                .profesionalId(1L)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(10, 0))
                .intervaloMinutos(30)
                .centroSaludId(1L)
                .build();
    }

    @Test
    @DisplayName("Debe registrar disponibilidad y generar slots cuando los datos son válidos")
    void testConfigurarDisponibilidad_Exitoso() {
        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(profesional));
        when(disponibilidadRepositorio.findSolapadas(anyLong(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(disponibilidadRepositorio.save(any(DisponibilidadProfesional.class))).thenAnswer(invocation -> {
            DisponibilidadProfesional entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        // Simular para el generador de slots
        when(feriadoServicio.esFeriado(any(LocalDate.class))).thenReturn(false);
        DisponibilidadProfesional dispActiva = DisponibilidadProfesional.builder()
                .id(10L)
                .profesional(profesional)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(10, 0))
                .intervaloMinutos(30)
                .activo(true)
                .build();

        when(disponibilidadRepositorio.findByProfesionalIdAndDiaSemanaAndActivoTrue(eq(1L), any(DayOfWeek.class)))
                .thenAnswer(invocation -> {
                    DayOfWeek d = invocation.getArgument(1);
                    return d == DayOfWeek.MONDAY ? List.of(dispActiva) : Collections.emptyList();
                });
        when(bloqueoAgendaRepositorio.findBloqueosParaFechaHora(anyLong(), any())).thenReturn(Collections.emptyList());
        when(turnoRepositorio.existsByProfesionalIdAndFechaHora(anyLong(), any())).thenReturn(false);

        DisponibilidadDtoResponse response = disponibilidadServicio.configurarDisponibilidad(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(1L, response.getProfesionalId());
        assertTrue(response.getSlotsGenerados() > 0);

        verify(disponibilidadRepositorio).save(any(DisponibilidadProfesional.class));
        verify(turnoRepositorio, atLeastOnce()).saveAll(anyList());
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException si la hora de inicio es igual o posterior a la hora de fin")
    void testConfigurarDisponibilidad_HoraInvalida() {
        request.setHoraInicio(LocalTime.of(14, 0));
        request.setHoraFin(LocalTime.of(10, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                disponibilidadServicio.configurarDisponibilidad(request));

        assertTrue(ex.getMessage().contains("debe ser estrictamente anterior"));
    }

    @Test
    @DisplayName("Debe lanzar IllegalStateException si ya existe solapamiento para ese día y profesional")
    void testConfigurarDisponibilidad_Solapamiento() {
        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(profesional));
        DisponibilidadProfesional existente = new DisponibilidadProfesional();
        existente.setId(99L);
        when(disponibilidadRepositorio.findSolapadas(anyLong(), any(), any(), any())).thenReturn(List.of(existente));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                disponibilidadServicio.configurarDisponibilidad(request));

        assertTrue(ex.getMessage().contains("conflicto de horarios"));
        verify(disponibilidadRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar NoEncontradoExcepcion si el profesional no existe")
    void testConfigurarDisponibilidad_ProfesionalNoEncontrado() {
        when(profesionalRepositorio.findById(999L)).thenReturn(Optional.empty());
        request.setProfesionalId(999L);

        assertThrows(NoEncontradoExcepcion.class, () ->
                disponibilidadServicio.configurarDisponibilidad(request));
    }

    @Test
    @DisplayName("Generación de slots debe omitir fechas feriadas y fechas bloqueadas")
    void testGenerarSlots_OmiteFeriadosYBloqueos() {
        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(profesional));

        DisponibilidadProfesional disp = DisponibilidadProfesional.builder()
                .id(1L)
                .profesional(profesional)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(9, 0))
                .intervaloMinutos(30)
                .activo(true)
                .build();

        // Que todos los lunes tengan disponibilidad
        when(disponibilidadRepositorio.findByProfesionalIdAndDiaSemanaAndActivoTrue(eq(1L), any(DayOfWeek.class)))
                .thenAnswer(invocation -> {
                    DayOfWeek d = invocation.getArgument(1);
                    return d == DayOfWeek.MONDAY ? List.of(disp) : Collections.emptyList();
                });

        // Configurar para que un lunes sea feriado
        when(feriadoServicio.esFeriado(any(LocalDate.class))).thenAnswer(invocation -> {
            LocalDate date = invocation.getArgument(0);
            return date.getDayOfWeek() == DayOfWeek.MONDAY && date.getDayOfMonth() <= 7;
        });

        // Configurar un bloqueo simulado
        when(bloqueoAgendaRepositorio.findBloqueosParaFechaHora(anyLong(), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    LocalDateTime dt = invocation.getArgument(1);
                    return dt.getHour() == 8 && dt.getMinute() == 0 ? List.of(new BloqueoAgenda()) : Collections.emptyList();
                });

        when(turnoRepositorio.existsByProfesionalIdAndFechaHora(anyLong(), any())).thenReturn(false);

        int slotsGenerados = disponibilidadServicio.generarSlotsProximos30Dias(1L);

        // Se generaron slots pero ninguno en el slot bloqueado ni en el feriado
        verify(turnoRepositorio).saveAll(argThat(turnos -> {
            List<Turno> lista = (List<Turno>) turnos;
            for (Turno t : lista) {
                // Ninguno debe ser a las 8:00 (bloqueado)
                assertNotEquals(LocalTime.of(8, 0), t.getFechaHora().toLocalTime());
            }
            return true;
        }));
    }
}
