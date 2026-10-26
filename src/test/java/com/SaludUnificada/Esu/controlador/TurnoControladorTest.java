package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.RecordatorioTurnoDtoResponse;
import com.SaludUnificada.Esu.servicio.ITurnoServicio;
import com.SaludUnificada.Esu.servicio.RecordatorioTurnoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TurnoControladorTest {
    @Test
    void disponibilidadExponeElArrayDeSlotsDelServicio() {
        ITurnoServicio service = mock(ITurnoServicio.class);
        TurnoControlador controller = new TurnoControlador();
        ReflectionTestUtils.setField(controller, "turnoServicio", service);
        LocalDate date = LocalDate.of(2026, 10, 15);
        List<LocalDateTime> slots = List.of(date.atTime(8, 0));
        when(service.obtenerHorariosDisponibles(3L, date)).thenReturn(slots);

        var response = controller.obtenerDisponibilidad(3L, date);

        assertEquals(slots, response.getBody());
        verify(service).obtenerHorariosDisponibles(3L, date);
    }

    @Test
    void crearTurnoPasaLaIdentidadAutenticadaAlServicio() {
        ITurnoServicio service = mock(ITurnoServicio.class);
        TurnoControlador controller = new TurnoControlador();
        ReflectionTestUtils.setField(controller, "turnoServicio", service);
        Principal principal = () -> "patient@example.test";

        controller.crearTurno(null, principal);

        verify(service).crearTurno(null, "patient@example.test");
    }

    @Test
    void recordatoriosSeListanParaElUsuarioAutenticado() {
        ITurnoServicio turnos = mock(ITurnoServicio.class);
        RecordatorioTurnoServicio reminders = mock(RecordatorioTurnoServicio.class);
        TurnoControlador controller = new TurnoControlador();
        ReflectionTestUtils.setField(controller, "turnoServicio", turnos);
        ReflectionTestUtils.setField(controller, "recordatorioTurnoServicio", reminders);
        Principal principal = () -> "patient@example.test";
        var result = List.of(new RecordatorioTurnoDtoResponse(1L, 2L, LocalDateTime.of(2026, 10, 15, 8, 0),
                "Profesional", LocalDateTime.of(2026, 10, 14, 8, 0), false));
        when(reminders.listarParaPaciente("patient@example.test")).thenReturn(result);

        var response = controller.listarRecordatorios(principal);

        assertEquals(result, response.getBody());
    }
}
