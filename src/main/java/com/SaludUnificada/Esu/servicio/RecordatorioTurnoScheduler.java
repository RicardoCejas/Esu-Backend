package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.RecordatorioTurno;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.repositorio.RecordatorioTurnoRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class RecordatorioTurnoScheduler {
    private final TurnoRepositorio turnoRepositorio;
    private final RecordatorioTurnoRepositorio recordatorioRepositorio;
    private final Clock clock;
    private final ZoneId zoneId;

    public RecordatorioTurnoScheduler(TurnoRepositorio turnoRepositorio,
                                     RecordatorioTurnoRepositorio recordatorioRepositorio,
                                     Clock clock, ZoneId turnosZoneId) {
        this.turnoRepositorio = turnoRepositorio;
        this.recordatorioRepositorio = recordatorioRepositorio;
        this.clock = clock;
        this.zoneId = turnosZoneId;
    }

    @Scheduled(fixedDelayString = "${esu.recordatorios.fixed-delay-ms:60000}")
    @Transactional
    public void crearRecordatoriosPendientes() {
        LocalDateTime ahora = LocalDateTime.now(clock.withZone(zoneId));
        for (Turno turno : turnoRepositorio.findByFechaHoraBetween(ahora, ahora.plusDays(2))) {
            if (!"CANCELADO".equalsIgnoreCase(turno.getEstado()) && !recordatorioRepositorio.existsByTurnoId(turno.getId())) {
                recordatorioRepositorio.save(new RecordatorioTurno(turno));
            }
        }
    }
}
