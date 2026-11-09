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
        RecordatorioTurnoServicio reminders = mock(RecordatorioTurnoServicio.class);
        TurnoControlador controller = new TurnoControlador(service, reminders);
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
        RecordatorioTurnoServicio reminders = mock(RecordatorioTurnoServicio.class);
        TurnoControlador controller = new TurnoControlador(service, reminders);
        Principal principal = () -> "patient@example.test";

        controller.crearTurno(null, principal);

        verify(service).crearTurno(null, "patient@example.test");
    }

    @Test
    void recordatoriosSeListanParaElUsuarioAutenticado() {
        ITurnoServicio turnos = mock(ITurnoServicio.class);
        RecordatorioTurnoServicio reminders = mock(RecordatorioTurnoServicio.class);
        TurnoControlador controller = new TurnoControlador(turnos, reminders);
        Principal principal = () -> "patient@example.test";
        var result = List.of(new RecordatorioTurnoDtoResponse(1L, 2L, LocalDateTime.of(2026, 10, 15, 8, 0),
                "Profesional", LocalDateTime.of(2026, 10, 14, 8, 0), false));
        when(reminders.listarParaPaciente("patient@example.test")).thenReturn(result);

        var response = controller.listarRecordatorios(principal);

        assertEquals(result, response.getBody());
    }
    @Test
    void obtenerTurnosHoyCentroDelegaEnServicio() {
        ITurnoServicio service = mock(ITurnoServicio.class);
        RecordatorioTurnoServicio reminders = mock(RecordatorioTurnoServicio.class);
        TurnoControlador controller = new TurnoControlador(service, reminders);
        LocalDate date = LocalDate.of(2026, 11, 9);
        when(service.obtenerTurnosHoyCentro(1L, date)).thenReturn(List.of());

        var response = controller.obtenerTurnosHoyCentro(1L, date);

        assertEquals(200, response.getStatusCode().value());
        verify(service).obtenerTurnosHoyCentro(1L, date);
    }

    @Test
    void obtenerTurnosKanbanCentroDelegaEnServicio() {
        ITurnoServicio service = mock(ITurnoServicio.class);
        RecordatorioTurnoServicio reminders = mock(RecordatorioTurnoServicio.class);
        TurnoControlador controller = new TurnoControlador(service, reminders);
        LocalDate date = LocalDate.of(2026, 11, 9);
        when(service.obtenerTurnosKanbanCentro(2L, date)).thenReturn(List.of());

        var response = controller.obtenerTurnosKanbanCentro(2L, date);

        assertEquals(200, response.getStatusCode().value());
        verify(service).obtenerTurnosKanbanCentro(2L, date);
    }
    @Test
    void realizarCheckInDelegaEnServicio() {
        ITurnoServicio service = mock(ITurnoServicio.class);
        RecordatorioTurnoServicio reminders = mock(RecordatorioTurnoServicio.class);
        TurnoControlador controller = new TurnoControlador(service, reminders);
        Principal principal = () -> "recepcion@crespo.test";
        when(service.realizarCheckIn(10L, "recepcion@crespo.test")).thenReturn(new com.SaludUnificada.Esu.dto.response.TurnoDtoResponse());

        var response = controller.realizarCheckIn(10L, principal);

        assertEquals(200, response.getStatusCode().value());
        verify(service).realizarCheckIn(10L, "recepcion@crespo.test");
    }

    @Test
    void llamarPacienteDelegaEnServicio() {
        ITurnoServicio service = mock(ITurnoServicio.class);
        RecordatorioTurnoServicio reminders = mock(RecordatorioTurnoServicio.class);
        TurnoControlador controller = new TurnoControlador(service, reminders);
        Principal principal = () -> "recepcion@crespo.test";
        when(service.llamarPaciente(10L, "102", "recepcion@crespo.test")).thenReturn(new com.SaludUnificada.Esu.dto.response.TurnoDtoResponse());

        var response = controller.llamarPaciente(10L, java.util.Map.of("consultorio", "102"), principal);

        assertEquals(200, response.getStatusCode().value());
        verify(service).llamarPaciente(10L, "102", "recepcion@crespo.test");
    }

    @Test
    void registrarCopagoDelegaEnServicio() {
        ITurnoServicio service = mock(ITurnoServicio.class);
        RecordatorioTurnoServicio reminders = mock(RecordatorioTurnoServicio.class);
        TurnoControlador controller = new TurnoControlador(service, reminders);
        com.SaludUnificada.Esu.dto.request.CopagoDtoRequest copago = new com.SaludUnificada.Esu.dto.request.CopagoDtoRequest();
        when(service.registrarCopago(10L, copago)).thenReturn(new com.SaludUnificada.Esu.dto.response.TurnoDtoResponse());

        var response = controller.registrarCopago(10L, copago);

        assertEquals(200, response.getStatusCode().value());
        verify(service).registrarCopago(10L, copago);
    }
}
