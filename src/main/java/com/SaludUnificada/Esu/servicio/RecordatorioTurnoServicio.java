package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.RecordatorioTurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.RecordatorioTurno;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.RecordatorioTurnoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class RecordatorioTurnoServicio {
    private final PacienteRepositorio pacienteRepositorio;
    private final RecordatorioTurnoRepositorio recordatorioRepositorio;
    private final Clock clock;
    private final ZoneId zoneId;

    public RecordatorioTurnoServicio(PacienteRepositorio pacienteRepositorio,
                                     RecordatorioTurnoRepositorio recordatorioRepositorio,
                                     Clock clock, ZoneId turnosZoneId) {
        this.pacienteRepositorio = pacienteRepositorio;
        this.recordatorioRepositorio = recordatorioRepositorio;
        this.clock = clock;
        this.zoneId = turnosZoneId;
    }

    @Transactional(readOnly = true)
    public List<RecordatorioTurnoDtoResponse> listarParaPaciente(String emailAutenticado) {
        Long pacienteId = pacienteRepositorio.findByUsuarioEmail(emailAutenticado)
                .orElseThrow(() -> new NoEncontradoExcepcion("El usuario autenticado no tiene un perfil de paciente vinculado.")).getId();
        LocalDateTime ahora = LocalDateTime.now(clock.withZone(zoneId));
        return recordatorioRepositorio
                .findByTurnoPacienteIdAndDisponibleDesdeLessThanEqualOrderByDisponibleDesdeAsc(pacienteId, ahora)
                .stream().filter(recordatorio -> !"CANCELADO".equalsIgnoreCase(recordatorio.getTurno().getEstado()))
                .map(this::toResponse).toList();
    }

    private RecordatorioTurnoDtoResponse toResponse(RecordatorioTurno recordatorio) {
        var turno = recordatorio.getTurno();
        return new RecordatorioTurnoDtoResponse(recordatorio.getId(), turno.getId(), turno.getFechaHora(),
                turno.getProfesional().getNombre() + " " + turno.getProfesional().getApellido(),
                recordatorio.getDisponibleDesde(), recordatorio.isLeido());
    }
}
