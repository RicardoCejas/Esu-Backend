package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.repositorio.RecordatorioTurnoRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class RecordatorioTurnoSchedulerTest {
    @Test
    void schedulerEsIdempotenteCuandoYaExisteElRecordatorio() {
        ZoneId zone = ZoneId.of("America/Argentina/Cordoba");
        Clock clock = Clock.fixed(Instant.parse("2026-10-14T13:00:00Z"), zone);
        TurnoRepositorio turnos = mock(TurnoRepositorio.class);
        RecordatorioTurnoRepositorio recordatorios = mock(RecordatorioTurnoRepositorio.class);
        Turno turno = new Turno(); turno.setId(7L); turno.setEstado("PENDIENTE");
        when(turnos.findByFechaHoraBetween(any(), any())).thenReturn(List.of(turno));
        when(recordatorios.existsByTurnoId(7L)).thenReturn(true);
        var scheduler = new RecordatorioTurnoScheduler(turnos, recordatorios, clock, zone);

        scheduler.crearRecordatoriosPendientes();

        verify(recordatorios, never()).save(any());
    }

    @Test
    void schedulerCreaUnRecordatorioCuandoFalta() {
        ZoneId zone = ZoneId.of("America/Argentina/Cordoba");
        Clock clock = Clock.fixed(Instant.parse("2026-10-14T13:00:00Z"), zone);
        TurnoRepositorio turnos = mock(TurnoRepositorio.class);
        RecordatorioTurnoRepositorio recordatorios = mock(RecordatorioTurnoRepositorio.class);
        Turno turno = new Turno(); turno.setId(8L); turno.setEstado("PENDIENTE");
        turno.setFechaHora(LocalDateTime.of(2026, 10, 15, 8, 0));
        when(turnos.findByFechaHoraBetween(any(), any())).thenReturn(List.of(turno));
        when(recordatorios.existsByTurnoId(8L)).thenReturn(false);
        var scheduler = new RecordatorioTurnoScheduler(turnos, recordatorios, clock, zone);

        scheduler.crearRecordatoriosPendientes();

        verify(recordatorios).save(argThat(reminder -> reminder.getTurno() == turno));
    }
}
