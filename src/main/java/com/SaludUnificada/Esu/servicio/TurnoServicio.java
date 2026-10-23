package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.TurnoMapper;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TurnoServicio implements ITurnoServicio {

    @Autowired
    private TurnoRepositorio turnoRepositorio;

    @Autowired
    private TurnoMapper turnoMapper;

    @Autowired
    private EmailNotificacionServicio emailNotificacionServicio;

    @Override
    public TurnoDtoResponse crearTurno(TurnoDtoRequest turnoDto) {
        Turno turno = turnoMapper.paraEntidad(turnoDto);
        if (turno.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se pueden crear turnos con fechas u horarios anteriores al momento actual.");
        }
        Turno turnoGuardado = turnoRepositorio.save(turno);

        // HU-97: Envío asíncrono de confirmación de turno con link de cancelación
        try {
            emailNotificacionServicio.enviarConfirmacionTurno(turnoGuardado);
        } catch (Exception ignored) {
            // Manejo preventivo de fallo sin abortar la transacción principal
        }

        return turnoMapper.paraDto(turnoGuardado);
    }

    @Override
    public TurnoDtoResponse obtenerTurnoPorId(Long id) {
        Turno turno = turnoRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + id));
        return turnoMapper.paraDto(turno);
    }

    @Override
    public List<TurnoDtoResponse> listarTodos() {
        return turnoRepositorio.findAll().stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminarTurno(Long id) {
        if (turnoRepositorio.existsById(id)) {
            turnoRepositorio.deleteById(id);
        } else {
            throw new NoEncontradoExcepcion("Turno no encontrado con ID: " + id);
        }
    }

    @Override
    public List<TurnoDtoResponse> filtrarTurnos(Long profesionalId, LocalDateTime fechaHora) {
        return turnoRepositorio.findByProfesionalIdAndFechaHora(profesionalId, fechaHora).stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TurnoDtoResponse> obtenerTurnosPorPaciente(Long pacienteId) {
        return turnoRepositorio.findByPacienteIdOrderByFechaHoraDesc(pacienteId).stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }
}
