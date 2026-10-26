package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.RecordatorioTurno;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.mapper.TurnoMapper;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.RecordatorioTurnoRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TurnoServicio implements ITurnoServicio {
    private static final String CANCELADO = "CANCELADO";
    public static final int LIMITE_SOBRETURNOS_DIARIOS = 3;

    private final TurnoRepositorio turnoRepositorio;
    private final TurnoMapper turnoMapper;
    private final PacienteRepositorio pacienteRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final Clock clock;
    private final ZoneId zoneId;
    private final RecordatorioTurnoRepositorio recordatorioRepositorio;
    private final LocalTime apertura;
    private final LocalTime cierre;
    private final int duracionSlotMinutos;

    @Autowired(required = false)
    private EmailNotificacionServicio emailNotificacionServicio;

    public TurnoServicio(TurnoRepositorio turnoRepositorio, TurnoMapper turnoMapper,
                         PacienteRepositorio pacienteRepositorio, ProfesionalRepositorio profesionalRepositorio,
                         RecordatorioTurnoRepositorio recordatorioRepositorio, Clock clock, ZoneId turnosZoneId,
                         @Value("${esu.turnos.open:08:00}") LocalTime apertura,
                         @Value("${esu.turnos.close:17:00}") LocalTime cierre,
                         @Value("${esu.turnos.slot-minutes:30}") int duracionSlotMinutos) {
        if (duracionSlotMinutos <= 0 || !cierre.isAfter(apertura)) {
            throw new IllegalArgumentException("Turno configuration requires a positive slot duration and close time after open time.");
        }
        this.turnoRepositorio = turnoRepositorio;
        this.turnoMapper = turnoMapper;
        this.pacienteRepositorio = pacienteRepositorio;
        this.profesionalRepositorio = profesionalRepositorio;
        this.recordatorioRepositorio = recordatorioRepositorio;
        this.clock = clock;
        this.zoneId = turnosZoneId;
        this.apertura = apertura;
        this.cierre = cierre;
        this.duracionSlotMinutos = duracionSlotMinutos;
    }

    @Override
    @Transactional
    public TurnoDtoResponse crearTurno(TurnoDtoRequest turnoDto, String emailAutenticado) {
        LocalDateTime fechaHora = turnoDto.getFechaHora();
        validarHorario(fechaHora);

        Profesional profesional = profesionalRepositorio.findByIdForUpdate(turnoDto.getProfesionalId())
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + turnoDto.getProfesionalId()));
        if (turnoRepositorio.existsByProfesionalIdAndFechaHoraAndEstadoNot(profesional.getId(), fechaHora, CANCELADO)) {
            throw new IllegalStateException("El horario solicitado ya no está disponible.");
        }

        Paciente paciente = pacienteRepositorio.findByUsuarioEmail(emailAutenticado)
                .orElseThrow(() -> new NoEncontradoExcepcion("El usuario autenticado no tiene un perfil de paciente vinculado."));

        Turno turno = new Turno();
        turno.setFechaHora(fechaHora);
        turno.setMotivoTurno(turnoDto.getMotivoTurno());
        turno.setEstado("PENDIENTE");
        turno.setPaciente(paciente);
        turno.setProfesional(profesional);
        Turno guardado = turnoRepositorio.save(turno);
        recordatorioRepositorio.save(new RecordatorioTurno(guardado));

        // HU-97: Envío asíncrono de confirmación de turno con link de cancelación
        if (emailNotificacionServicio != null) {
            try {
                emailNotificacionServicio.enviarConfirmacionTurno(guardado);
            } catch (Exception ignored) {
                // Manejo preventivo de fallo sin abortar la transacción principal
            }
        }

        return turnoMapper.paraDto(guardado);
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
    public List<LocalDateTime> obtenerHorariosDisponibles(Long profesionalId, LocalDate fecha) {
        if (!profesionalRepositorio.existsById(profesionalId)) {
            throw new NoEncontradoExcepcion("Profesional no encontrado con ID: " + profesionalId);
        }
        if (fecha.isBefore(LocalDate.now(clock.withZone(zoneId))) || fecha.getDayOfWeek().getValue() > 5) {
            return List.of();
        }

        LocalDateTime inicio = fecha.atTime(apertura);
        LocalDateTime fin = fecha.atTime(cierre);
        List<LocalDateTime> ocupados = turnoRepositorio
                .findByProfesionalIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
                        profesionalId, inicio, fin, CANCELADO)
                .stream().map(Turno::getFechaHora).collect(Collectors.toList());

        LocalDateTime ahora = LocalDateTime.now(clock.withZone(zoneId));
        List<LocalDateTime> disponibles = new ArrayList<>();
        for (LocalDateTime slot = inicio; slot.plusMinutes(duracionSlotMinutos).compareTo(fin) <= 0;
             slot = slot.plusMinutes(duracionSlotMinutos)) {
            if (slot.isAfter(ahora) && !ocupados.contains(slot)) {
                disponibles.add(slot);
            }
        }
        return disponibles;
    }

    private void validarHorario(LocalDateTime fechaHora) {
        LocalDateTime ahora = LocalDateTime.now(clock.withZone(zoneId));
        boolean diaHabil = fechaHora.getDayOfWeek().getValue() <= 5;
        long minutosDesdeApertura = java.time.Duration.between(apertura, fechaHora.toLocalTime()).toMinutes();
        boolean dentroDeJornada = !fechaHora.toLocalTime().isBefore(apertura)
                && !fechaHora.toLocalTime().plusMinutes(duracionSlotMinutos).isAfter(cierre)
                && minutosDesdeApertura % duracionSlotMinutos == 0
                && fechaHora.getSecond() == 0 && fechaHora.getNano() == 0;
        if (!fechaHora.isAfter(ahora) || !diaHabil || !dentroDeJornada) {
            throw new IllegalArgumentException("El turno debe ser futuro, en día hábil y respetar la jornada y duración de slots configuradas.");
        }
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
        return turnoRepositorio.findAll().stream().map(turnoMapper::paraDto).collect(Collectors.toList());
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
                .map(turnoMapper::paraDto).collect(Collectors.toList());
    }
}
