package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.mapper.TurnoMapper;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TurnoServicio implements ITurnoServicio {

    public static final int LIMITE_SOBRETURNOS_DIARIOS = 3;

    private final TurnoRepositorio turnoRepositorio;
    private final TurnoMapper turnoMapper;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final EmailNotificacionServicio emailNotificacionServicio;

    @Override
    @Transactional
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
    @Transactional
    public TurnoDtoResponse crearSobreturno(SobreturnoDtoRequest request) {
        Profesional profesional = profesionalRepositorio.findById(request.getProfesionalId())
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + request.getProfesionalId()));

        Paciente paciente = pacienteRepositorio.findById(request.getPacienteId())
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + request.getPacienteId()));

        LocalDateTime inicioDia = request.getFechaHora().toLocalDate().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1).minusNanos(1);

        long sobreturnosHoy = turnoRepositorio.contarSobreturnosEnDia(request.getProfesionalId(), inicioDia, finDia);
        if (sobreturnosHoy >= LIMITE_SOBRETURNOS_DIARIOS) {
            throw new ReglaNegocioException("Se ha superado el cupo máximo de sobreturnos (" +
                    LIMITE_SOBRETURNOS_DIARIOS + " por día) para el profesional en la jornada indicada.");
        }

        Turno sobreturno = new Turno();
        sobreturno.setFechaHora(request.getFechaHora());
        sobreturno.setEstado("PENDIENTE");
        sobreturno.setMotivoTurno(request.getMotivoSobreturno());
        sobreturno.setEsSobreturno(true);
        sobreturno.setMotivoSobreturno(request.getMotivoSobreturno());
        sobreturno.setProfesional(profesional);
        sobreturno.setPaciente(paciente);

        Turno guardado = turnoRepositorio.save(sobreturno);
        return turnoMapper.paraDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public TurnoDtoResponse obtenerTurnoPorId(Long id) {
        Turno turno = turnoRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + id));
        return turnoMapper.paraDto(turno);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoDtoResponse> listarTodos() {
        return turnoRepositorio.findAll().stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void eliminarTurno(Long id) {
        if (turnoRepositorio.existsById(id)) {
            turnoRepositorio.deleteById(id);
        } else {
            throw new NoEncontradoExcepcion("Turno no encontrado con ID: " + id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoDtoResponse> filtrarTurnos(Long profesionalId, LocalDateTime fechaHora) {
        return turnoRepositorio.findByProfesionalIdAndFechaHora(profesionalId, fechaHora).stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoDtoResponse> obtenerTurnosPorPaciente(Long pacienteId) {
        return turnoRepositorio.findByPacienteIdOrderByFechaHoraDesc(pacienteId).stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }
}
