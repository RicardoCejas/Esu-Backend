package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.CopagoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoMostradorDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDisponibleDtoResponse;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.CentroSalud;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.TurnoMapper;
import com.SaludUnificada.Esu.repositorio.CentroSaludRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class TurnoServicio implements ITurnoServicio {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PENDIENTE", "CONFIRMADO", "EN_ESPERA", "EN_SALA_DE_ESPERA",
            "ATENDIENDO", "COMPLETADO", "FINALIZADO", "CANCELADO", "AUSENTE");

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final LocalTime INICIO_AGENDA = LocalTime.of(8, 0);
    private static final LocalTime FIN_AGENDA = LocalTime.of(18, 0);

    @Autowired
    private TurnoRepositorio turnoRepositorio;

    @Autowired
    private TurnoMapper turnoMapper;

    @Autowired
    private PacienteRepositorio pacienteRepositorio;

    @Autowired
    private ProfesionalRepositorio profesionalRepositorio;

    @Autowired
    private CentroSaludRepositorio centroSaludRepositorio;

    @Override
    public TurnoDtoResponse crearTurno(TurnoDtoRequest turnoDto) {
        Turno turno = turnoMapper.paraEntidad(turnoDto);
        if (turno.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se pueden crear turnos con fechas u horarios anteriores al momento actual.");
        }
        if (turno.getProfesional() != null
                && turnoRepositorio.existsByProfesionalIdAndFechaHoraAndEstadoNot(
                        turno.getProfesional().getId(), turno.getFechaHora(), "CANCELADO")) {
            throw new IllegalStateException("El profesional ya tiene un turno asignado en ese horario.");
        }
        Turno turnoGuardado = turnoRepositorio.save(turno);
        return turnoMapper.paraDto(turnoGuardado);
    }

    @Override
    public TurnoDtoResponse obtenerTurnoPorId(Long id) {
        return turnoMapper.paraDto(obtenerEntidad(id));
    }

    @Override
    public List<TurnoDtoResponse> listarTodos() {
        return turnoRepositorio.findAll().stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminarTurno(Long id) {
        if (!turnoRepositorio.existsById(id)) {
            throw new NoEncontradoExcepcion("Turno no encontrado con ID: " + id);
        }
        turnoRepositorio.deleteById(id);
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

    @Override
    public List<TurnoDtoResponse> obtenerTurnosPorProfesional(Long profesionalId, LocalDate fecha) {
        List<Turno> turnos;
        if (fecha == null) {
            turnos = turnoRepositorio.findByProfesionalIdOrderByFechaHoraAsc(profesionalId);
        } else {
            turnos = turnoRepositorio.findByProfesionalIdAndFechaHoraBetween(
                    profesionalId,
                    fecha.atStartOfDay(),
                    fecha.atTime(23, 59, 59));
        }
        return turnos.stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TurnoDisponibleDtoResponse> obtenerDisponibles(Long profesionalId, LocalDate fecha) {
        Profesional profesional = profesionalRepositorio.findById(profesionalId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + profesionalId));
        LocalDate dia = fecha == null ? LocalDate.now() : fecha;
        int duracion = profesional.getDuracionTurnoMin() == null || profesional.getDuracionTurnoMin() <= 0
                ? 20
                : profesional.getDuracionTurnoMin();

        List<TurnoDisponibleDtoResponse> disponibles = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime cursor = dia.atTime(INICIO_AGENDA);
        LocalDateTime fin = dia.atTime(FIN_AGENDA);

        while (cursor.isBefore(fin)) {
            if (cursor.isAfter(ahora)
                    && !turnoRepositorio.existsByProfesionalIdAndFechaHoraAndEstadoNot(
                            profesionalId, cursor, "CANCELADO")) {
                disponibles.add(new TurnoDisponibleDtoResponse(
                        cursor,
                        cursor.format(FORMATO_HORA),
                        true));
            }
            cursor = cursor.plusMinutes(duracion);
        }

        return disponibles;
    }

    @Override
    public TurnoDtoResponse actualizarEstado(Long id, String estado) {
        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException("El estado del turno es obligatorio.");
        }
        String normalizado = estado.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("Estado de turno invalido: " + estado);
        }
        Turno turno = obtenerEntidad(id);
        turno.setEstado(normalizado);
        return turnoMapper.paraDto(turnoRepositorio.save(turno));
    }

    @Override
    public TurnoDtoResponse registrarCheckIn(Long id, String horaCheckIn) {
        Turno turno = obtenerEntidad(id);
        turno.setHoraCheckIn(horaValidaONow(horaCheckIn));
        turno.setEstado("EN_SALA_DE_ESPERA");
        return turnoMapper.paraDto(turnoRepositorio.save(turno));
    }

    @Override
    public TurnoDtoResponse registrarCopago(Long id, CopagoDtoRequest request) {
        Turno turno = obtenerEntidad(id);
        if (request.getObraSocial() != null) {
            turno.setObraSocial(recortar(request.getObraSocial()));
        }
        if (request.getNumeroAfiliado() != null) {
            turno.setNumeroAfiliado(recortar(request.getNumeroAfiliado()));
        }
        if (request.getCopagoMonto() != null) {
            turno.setCopagoMonto(request.getCopagoMonto());
        }
        if (request.getNumeroBono() != null) {
            turno.setNumeroBono(recortar(request.getNumeroBono()));
        }
        return turnoMapper.paraDto(turnoRepositorio.save(turno));
    }

    @Override
    public TurnoDtoResponse crearTurnoMostrador(TurnoMostradorDtoRequest request) {
        Long pacienteId = parsearId(request.getPacienteId(), "paciente");
        Long profesionalId = parsearId(request.getProfesionalId(), "profesional");
        if (pacienteId == null || profesionalId == null) {
            throw new IllegalArgumentException("El paciente y el profesional son obligatorios.");
        }

        Paciente paciente = pacienteRepositorio.findById(pacienteId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId));
        Profesional profesional = profesionalRepositorio.findById(profesionalId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + profesionalId));

        Turno turno = new Turno();
        turno.setPaciente(paciente);
        turno.setProfesional(profesional);
        turno.setFechaHora(parsearFechaHora(request));
        turno.setEstado(request.getEstado() == null || request.getEstado().isBlank()
                ? "PENDIENTE"
                : request.getEstado().trim().toUpperCase());
        turno.setMotivoTurno(recortar(request.getMotivoConsulta() != null
                ? request.getMotivoConsulta()
                : request.getMotivoTurno()));
        turno.setObraSocial(recortar(request.getObraSocial()));
        turno.setNumeroAfiliado(recortar(request.getNumeroAfiliado()));
        turno.setCopagoMonto(request.getCopagoMonto());
        turno.setNumeroBono(recortar(request.getNumeroBono()));
        turno.setEsPrimeraVez(Boolean.TRUE.equals(request.getEsPrimeraVez()));
        turno.setConsultorioNumero(recortar(request.getConsultorioNumero()));
        turno.setPlanta(recortar(request.getPlanta()));
        turno.setCodigoVerificacion(generarCodigoVerificacion());

        Long centroId = parsearId(request.getCentroSaludId(), "centro de salud");
        if (centroId != null) {
            CentroSalud centro = centroSaludRepositorio.findById(centroId)
                    .orElseThrow(() -> new NoEncontradoExcepcion("Centro de salud no encontrado con ID: " + centroId));
            turno.setCentroSalud(centro);
        }

        return turnoMapper.paraDto(turnoRepositorio.save(turno));
    }

    @Override
    public TurnoDtoResponse cancelarConJustificativo(Long id, String justificativo) {
        Turno turno = obtenerEntidad(id);
        turno.setEstado("CANCELADO");
        turno.setJustificativoCancelacion(recortar(justificativo));
        return turnoMapper.paraDto(turnoRepositorio.save(turno));
    }

    @Override
    public TurnoDtoResponse llamarPaciente(Long id, String consultorio, String horaLlamado) {
        Turno turno = obtenerEntidad(id);
        turno.setEstado("ATENDIENDO");
        turno.setConsultorioNumero(recortar(consultorio));
        turno.setHoraLlamado(horaValidaONow(horaLlamado));
        return turnoMapper.paraDto(turnoRepositorio.save(turno));
    }

    @Override
    public void cancelarTurno(Long id) {
        Turno turno = obtenerEntidad(id);
        turno.setEstado("CANCELADO");
        turnoRepositorio.save(turno);
    }

    private Turno obtenerEntidad(Long id) {
        return turnoRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + id));
    }

    private Long parsearId(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El ID de " + campo + " no es valido: " + valor);
        }
    }

    private LocalDateTime parsearFechaHora(TurnoMostradorDtoRequest request) {
        if (request.getFechaHora() != null && !request.getFechaHora().isBlank()) {
            try {
                return LocalDateTime.parse(request.getFechaHora().trim());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("La fecha y hora del turno no es valida.");
            }
        }
        if (request.getFecha() == null || request.getFecha().isBlank()
                || request.getHora() == null || request.getHora().isBlank()) {
            throw new IllegalArgumentException("La fecha y la hora del turno son obligatorias.");
        }
        try {
            return LocalDateTime.parse(request.getFecha().trim() + "T" + request.getHora().trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha y hora del turno no es valida.");
        }
    }

    private String horaValidaONow(String hora) {
        if (hora == null || hora.isBlank()) {
            return LocalTime.now().format(FORMATO_HORA);
        }
        try {
            return LocalTime.parse(hora.trim(), FORMATO_HORA).format(FORMATO_HORA);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La hora debe tener el formato HH:mm.");
        }
    }

    private String generarCodigoVerificacion() {
        int aleatorio = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "ESU-" + LocalDate.now().getYear() + "-" + aleatorio;
    }

    private String recortar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
