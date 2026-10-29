package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.BloqueoAgendaDtoRequest;
import com.SaludUnificada.Esu.dto.response.BloqueoAgendaDtoResponse;
import com.SaludUnificada.Esu.dto.response.TurnoAfectadoDto;
import com.SaludUnificada.Esu.entidad.BloqueoAgenda;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.BloqueoAgendaMapper;
import com.SaludUnificada.Esu.repositorio.BloqueoAgendaRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BloqueoAgendaServicio implements IBloqueoAgendaServicio {

    private static final Logger log = LoggerFactory.getLogger(BloqueoAgendaServicio.class);

    private final BloqueoAgendaRepositorio bloqueoAgendaRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final TurnoRepositorio turnoRepositorio;
    private final BloqueoAgendaMapper bloqueoAgendaMapper;
    private final NotificacionUsuarioServicio notificacionUsuarioServicio;
    private final EmailNotificacionServicio emailNotificacionServicio;

    @Override
    @Transactional
    public BloqueoAgendaDtoResponse registrarBloqueo(Long profesionalId, BloqueoAgendaDtoRequest request) {
        if (!request.getFechaInicio().isBefore(request.getFechaFin())) {
            throw new IllegalArgumentException("La fecha y hora de inicio debe ser anterior a la fecha y hora de fin del bloqueo.");
        }

        Profesional profesional = profesionalRepositorio.findById(profesionalId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + profesionalId));

        BloqueoAgenda entidad = bloqueoAgendaMapper.paraEntidad(request, profesional);
        BloqueoAgenda guardado = bloqueoAgendaRepositorio.save(entidad);

        List<Turno> turnosSolapados = turnoRepositorio.findByProfesionalIdAndFechaHoraBetween(
                profesionalId,
                request.getFechaInicio(),
                request.getFechaFin()
        );

        List<TurnoAfectadoDto> turnosCancelados = new ArrayList<>();

        for (Turno turno : turnosSolapados) {
            String estadoAnterior = turno.getEstado();

            if (!"CANCELADO_POR_MEDICO".equalsIgnoreCase(estadoAnterior)) {
                turno.setEstado("CANCELADO_POR_MEDICO");

                if (turno.getPaciente() != null) {
                    turnosCancelados.add(TurnoAfectadoDto.builder()
                            .turnoId(turno.getId())
                            .fechaHora(turno.getFechaHora())
                            .pacienteId(turno.getPaciente().getId())
                            .nombrePaciente(turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido())
                            .dniPaciente(turno.getPaciente().getDni())
                            .telefonoPaciente(turno.getPaciente().getTelefono())
                            .estadoAnterior(estadoAnterior)
                            .build());
                }
            }
        }

        if (!turnosSolapados.isEmpty()) {
            turnoRepositorio.saveAll(turnosSolapados);
            for (Turno tCancelado : turnosSolapados) {
                if (tCancelado.getPaciente() != null && tCancelado.getPaciente().getUsuario() != null) {
                    if (notificacionUsuarioServicio != null) {
                        try {
                            notificacionUsuarioServicio.crearNotificacion(
                                    tCancelado.getPaciente().getUsuario(),
                                    "Turno Cancelado por Indisponibilidad Medica",
                                    "Su turno del " + (tCancelado.getFechaHora() != null ? tCancelado.getFechaHora().toString() : "") + " fue cancelado por bloqueo de agenda del profesional.",
                                    "TURNO",
                                    "/mis-turnos"
                            );
                        } catch (Exception ignored) {
                        }
                    }
                    if (emailNotificacionServicio != null) {
                        try {
                            emailNotificacionServicio.enviarCancelacionTurno(tCancelado, "Cancelacion por bloqueo de agenda del profesional: " + request.getMotivo());
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        log.info("Bloqueo de agenda registrado para profesional {}. Período: {} a {}. Turnos afectados: {}",
                profesionalId, request.getFechaInicio(), request.getFechaFin(), turnosCancelados.size());

        return bloqueoAgendaMapper.paraDto(guardado, turnosCancelados);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueoAgendaDtoResponse> listarBloqueosPorProfesional(Long profesionalId) {
        if (!profesionalRepositorio.existsById(profesionalId)) {
            throw new NoEncontradoExcepcion("Profesional no encontrado con ID: " + profesionalId);
        }

        return bloqueoAgendaRepositorio.findByProfesionalId(profesionalId)
                .stream()
                .map(bloqueoAgendaMapper::paraDto)
                .toList();
    }
}
