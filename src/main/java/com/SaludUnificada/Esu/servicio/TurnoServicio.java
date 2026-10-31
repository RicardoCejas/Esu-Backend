package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.AtencionTurnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.CopagoDtoRequest;
import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;

import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;

import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;

import com.SaludUnificada.Esu.entidad.Paciente;

import com.SaludUnificada.Esu.entidad.Profesional;

import com.SaludUnificada.Esu.entidad.RecordatorioTurno;

import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.entidad.TurnoEstado;

import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;

import com.SaludUnificada.Esu.error.ReglaNegocioException;

import com.SaludUnificada.Esu.mapper.TurnoMapper;

import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;

import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;

import com.SaludUnificada.Esu.repositorio.RecordatorioTurnoRepositorio;

import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import com.SaludUnificada.Esu.repositorio.BloqueoAgendaRepositorio;
import com.SaludUnificada.Esu.repositorio.DisponibilidadProfesionalRepositorio;
import com.SaludUnificada.Esu.entidad.DisponibilidadProfesional;
import com.SaludUnificada.Esu.entidad.BloqueoAgenda;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import com.SaludUnificada.Esu.seguridad.MascaraDatosUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

import java.time.LocalDate;

import java.time.LocalDateTime;

import java.time.LocalTime;

import java.time.ZoneId;

import java.util.ArrayList;

import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;

@Slf4j
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
    @Autowired(required = false)
    private NotificacionUsuarioServicio notificacionUsuarioServicio;

    @Autowired(required = false)
    private BloqueoAgendaRepositorio bloqueoAgendaRepositorio;

    @Autowired(required = false)
    private DocumentoPdfServicio documentoPdfServicio;

    @Autowired(required = false)
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired(required = false)
    private DisponibilidadProfesionalRepositorio disponibilidadRepositorio;

    @Autowired(required = false)
    private FeriadoServicio feriadoServicio;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

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
        if (turnoDto.getIdempotencyKey() != null && !turnoDto.getIdempotencyKey().isBlank()) {
            String key = turnoDto.getIdempotencyKey().trim();
            Optional<Turno> previo = turnoRepositorio.findByIdempotencyKey(key);
            if (previo.isPresent()) {
                validarCoincidenciaYAutorizacionIdempotencia(previo.get(), turnoDto.getPacienteId(), turnoDto.getProfesionalId(), turnoDto.getFechaHora(), false, emailAutenticado, key);
                log.info("Turno ya procesado previamente con idempotencyKey={}. Devolviendo instancia existente ID={}.",
                        key, previo.get().getId());
                return turnoMapper.paraDto(previo.get());
            }
        }

        LocalDateTime fechaHora = turnoDto.getFechaHora();

        validarHorario(fechaHora);
        LocalDateTime ahora = ahora();
        if (fechaHora.isBefore(ahora)) {
            throw new ReglaNegocioException("No es posible solicitar turnos medicos en fechas u horarios pasados.");
        }

        Profesional profesional = profesionalRepositorio.findByIdForUpdate(turnoDto.getProfesionalId())
                .or(() -> profesionalRepositorio.findById(turnoDto.getProfesionalId()))
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + turnoDto.getProfesionalId()));
        if (bloqueoAgendaRepositorio != null && !bloqueoAgendaRepositorio.findBloqueosParaFechaHora(profesional.getId(), fechaHora).isEmpty()) {
            throw new ReglaNegocioException("El profesional seleccionado no se encuentra disponible debido a un bloqueo de agenda activo (licencia o ausencia programada).");
        }

        if (disponibilidadRepositorio != null) {
            List<DisponibilidadProfesional> disponibilidades = disponibilidadRepositorio.findByProfesionalIdAndDiaSemanaAndActivoTrue(profesional.getId(), fechaHora.getDayOfWeek());
            if (disponibilidades.isEmpty()) {
                throw new ReglaNegocioException("El profesional no posee franjas de disponibilidad configuradas para el día seleccionado (agenda cerrada).");
            }
            boolean calza = disponibilidades.stream().anyMatch(disp -> {
                LocalTime t = fechaHora.toLocalTime();
                int dur = (disp.getIntervaloMinutos() != null && disp.getIntervaloMinutos() > 0) ? disp.getIntervaloMinutos() : duracionSlotMinutos;
                if (t.isBefore(disp.getHoraInicio()) || t.plusMinutes(dur).isAfter(disp.getHoraFin())) {
                    return false;
                }
                long mins = java.time.Duration.between(disp.getHoraInicio(), t).toMinutes();
                return mins % dur == 0;
            });
            if (!calza) {
                throw new ReglaNegocioException("El horario solicitado no coincide con la agenda y disponibilidad configurada para el profesional.");
            }
        }

        if (turnoRepositorio.existsByProfesionalIdAndFechaHoraAndEstadoNot(profesional.getId(), fechaHora, CANCELADO)) {

            throw new IllegalStateException("El horario solicitado ya no está disponible.");

        }

        Paciente paciente;
        if (emailAutenticado != null && !emailAutenticado.isBlank()) {
            Optional<Usuario> usuarioOpt = usuarioRepositorio != null ? usuarioRepositorio.findByEmail(emailAutenticado) : Optional.empty();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean esPaciente = (usuarioOpt.isPresent() && "PACIENTE".equalsIgnoreCase(usuarioOpt.get().getRol()))
                    || (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE")));

            if (esPaciente) {
                Paciente miPerfil = pacienteRepositorio.findByUsuarioEmail(emailAutenticado)
                        .orElseThrow(() -> new NoEncontradoExcepcion("El usuario autenticado no tiene un perfil de paciente vinculado."));
                if (turnoDto.getPacienteId() != null && !turnoDto.getPacienteId().equals(miPerfil.getId())) {
                    throw new AccessDeniedException("Un paciente no puede agendar turnos para otro paciente.");
                }
                paciente = miPerfil;
            } else if (turnoDto.getPacienteId() != null) {
                paciente = pacienteRepositorio.findById(turnoDto.getPacienteId())
                        .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + turnoDto.getPacienteId()));
            } else {
                paciente = pacienteRepositorio.findByUsuarioEmail(emailAutenticado)
                        .orElseThrow(() -> new NoEncontradoExcepcion("Debe indicarse el paciente para el turno."));
            }
        } else if (turnoDto.getPacienteId() != null) {
            paciente = pacienteRepositorio.findById(turnoDto.getPacienteId())
                    .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + turnoDto.getPacienteId()));
        } else {
            throw new IllegalArgumentException("Debe indicarse el paciente para el turno.");
        }

        if (turnoRepositorio.existsByPacienteIdAndFechaHoraAndEstadoNot(paciente.getId(), fechaHora, CANCELADO)) {
            throw new ReglaNegocioException("El paciente ya cuenta con un turno registrado en el mismo día y horario.");
        }

        Turno turno = new Turno();
        if (turnoDto.getIdempotencyKey() != null && !turnoDto.getIdempotencyKey().isBlank()) {
            turno.setIdempotencyKey(turnoDto.getIdempotencyKey().trim());
        }

        turno.setFechaHora(fechaHora);

        turno.setMotivoTurno(turnoDto.getMotivoTurno());

        turno.setEstado("PENDIENTE");

        turno.setPaciente(paciente);

        turno.setProfesional(profesional);

        Long centroId = turnoDto.getCentroSaludId();
        String centroNombre = turnoDto.getCentroSaludNombre();
        if (centroId == null && disponibilidadRepositorio != null) {
            Optional<DisponibilidadProfesional> matchingDisp = disponibilidadRepositorio
                    .findByProfesionalIdAndDiaSemanaAndActivoTrue(profesional.getId(), fechaHora.getDayOfWeek())
                    .stream()
                    .filter(disp -> {
                        LocalTime t = fechaHora.toLocalTime();
                        return !t.isBefore(disp.getHoraInicio()) && t.isBefore(disp.getHoraFin());
                    })
                    .findFirst();
            if (matchingDisp.isPresent()) {
                centroId = matchingDisp.get().getCentroSaludId();
            }
        }
        if (centroId == null) {
            centroId = 1L;
        }
        if (centroNombre == null || centroNombre.isBlank()) {
            centroNombre = resolverNombreCentro(centroId);
        }
        turno.setCentroSaludId(centroId);
        turno.setCentroSaludNombre(centroNombre);
        turno.setConsultorioNumero(turnoDto.getConsultorioNumero() != null ? turnoDto.getConsultorioNumero() : "Consultorio 1");

        Turno guardado;
        try {
            guardado = turnoRepositorio.save(turno);
            if (guardado != null) turnoRepositorio.flush();
        } catch (DataIntegrityViolationException dive) {
            if (turnoDto.getIdempotencyKey() != null && !turnoDto.getIdempotencyKey().isBlank()) {
                String key = turnoDto.getIdempotencyKey().trim();
                Optional<Turno> recuperado = turnoRepositorio.findByIdempotencyKey(key);
                if (recuperado.isPresent()) {
                    validarCoincidenciaYAutorizacionIdempotencia(recuperado.get(), paciente.getId(), profesional.getId(), fechaHora, false, emailAutenticado, key);
                    log.info("ColisiÃ³n simultÃ¡nea resuelta concurrentemente por idempotencyKey={}. Devolviendo turno ID={}.", key, recuperado.get().getId());
                    return turnoMapper.paraDto(recuperado.get());
                }
            }
            throw dive;
        }

        if (recordatorioRepositorio != null) {
            try {
                recordatorioRepositorio.deleteByTurnoId(guardado.getId());
                recordatorioRepositorio.save(new RecordatorioTurno(guardado));
            } catch (Exception ex) {
                log.error("Fallo al persistir recordatorio para el turno ID={}: {}. El reconciliador programado lo recuperarÃƒÂ¡.", guardado.getId(), ex.getMessage());
            }
        }
        if (notificacionUsuarioServicio != null && guardado.getPaciente() != null && guardado.getPaciente().getUsuario() != null) {
            notificacionUsuarioServicio.crearNotificacion(
                guardado.getPaciente().getUsuario(),
                "Turno Solicitado",
                "Su turno mÃƒÂ©dico para el " + guardado.getFechaHora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " fue registrado y se encuentra pendiente de confirmaciÃƒÂ³n.",
                "TURNO",
                "/mis-turnos"
            );
        }

        return turnoMapper.paraDto(guardado);

    }

    @Override

    @Transactional

    public TurnoDtoResponse crearSobreturno(SobreturnoDtoRequest request) {
        String emailAut = null;
        Authentication authSob = SecurityContextHolder.getContext().getAuthentication();
        if (authSob != null && authSob.isAuthenticated() && !"anonymousUser".equals(authSob.getName())) {
            emailAut = authSob.getName();
        }
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            String key = request.getIdempotencyKey().trim();
            Optional<Turno> previo = turnoRepositorio.findByIdempotencyKey(key);
            if (previo.isPresent()) {
                validarCoincidenciaYAutorizacionIdempotencia(previo.get(), request.getPacienteId(), request.getProfesionalId(), request.getFechaHora().withSecond(0).withNano(0), true, emailAut, key);
                log.info("Sobreturno ya procesado previamente con idempotencyKey={}. Devolviendo instancia existente ID={}.",
                        key, previo.get().getId());
                return turnoMapper.paraDto(previo.get());
            }
        }

        LocalDateTime ahora = ahora();
        LocalDateTime fechaHora = request.getFechaHora().withSecond(0).withNano(0);
        if (fechaHora.isBefore(ahora)) {
            throw new ReglaNegocioException("No es posible solicitar sobreturnos en fechas u horarios pasados.");
        }

        validarHorario(fechaHora);

        Profesional profesional = profesionalRepositorio.findByIdForUpdate(request.getProfesionalId())
                .or(() -> profesionalRepositorio.findById(request.getProfesionalId()))
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + request.getProfesionalId()));

        if (bloqueoAgendaRepositorio != null && !bloqueoAgendaRepositorio.findBloqueosParaFechaHora(profesional.getId(), fechaHora).isEmpty()) {
            throw new ReglaNegocioException("El profesional seleccionado no se encuentra disponible debido a un bloqueo de agenda activo (licencia o ausencia programada).");
        }

        if (disponibilidadRepositorio != null) {
            List<DisponibilidadProfesional> disponibilidades = disponibilidadRepositorio.findByProfesionalIdAndDiaSemanaAndActivoTrue(profesional.getId(), fechaHora.getDayOfWeek());
            if (disponibilidades.isEmpty()) {
                throw new ReglaNegocioException("El profesional no posee disponibilidad configurada para la jornada del sobreturno (agenda cerrada).");
            }
        }

        Paciente paciente = pacienteRepositorio.findById(request.getPacienteId())
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + request.getPacienteId()));

        if (turnoRepositorio.existsByPacienteIdAndFechaHoraAndEstadoNot(paciente.getId(), fechaHora, CANCELADO)) {
            throw new ReglaNegocioException("El paciente ya cuenta con un turno registrado en el mismo día y horario.");
        }

        LocalDateTime inicioDia = fechaHora.toLocalDate().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1).minusNanos(1);

        long sobreturnosHoy = turnoRepositorio.contarSobreturnosEnDia(request.getProfesionalId(), inicioDia, finDia);
        if (sobreturnosHoy >= LIMITE_SOBRETURNOS_DIARIOS) {
            throw new ReglaNegocioException("Se ha superado el cupo máximo de sobreturnos (" +
                    LIMITE_SOBRETURNOS_DIARIOS + " por día) para el profesional en la jornada indicada.");
        }

        Turno sobreturno = new Turno();
        sobreturno.setFechaHora(fechaHora);
        sobreturno.setEstado("PENDIENTE");
        sobreturno.setMotivoTurno(request.getMotivoSobreturno());
        sobreturno.setEsSobreturno(true);
        sobreturno.setMotivoSobreturno(request.getMotivoSobreturno());
        Long centroId = 1L;
        sobreturno.setCentroSaludId(centroId);
        sobreturno.setCentroSaludNombre(resolverNombreCentro(centroId));
        sobreturno.setConsultorioNumero("Consultorio Guardia / Sobreturnos");
        sobreturno.setProfesional(profesional);
        sobreturno.setPaciente(paciente);
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            sobreturno.setIdempotencyKey(request.getIdempotencyKey().trim());
        }

        Turno guardado;
        try {
            guardado = turnoRepositorio.save(sobreturno);
            if (guardado != null) turnoRepositorio.flush();
        } catch (DataIntegrityViolationException dive) {
            if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
                String key = request.getIdempotencyKey().trim();
                Optional<Turno> recuperado = turnoRepositorio.findByIdempotencyKey(key);
                if (recuperado.isPresent()) {
                    validarCoincidenciaYAutorizacionIdempotencia(recuperado.get(), paciente.getId(), profesional.getId(), fechaHora, true, emailAut, key);
                    log.info("ColisiÃ³n simultÃ¡nea de sobreturno resuelta concurrentemente por idempotencyKey={}. Devolviendo turno ID={}.", key, recuperado.get().getId());
                    return turnoMapper.paraDto(recuperado.get());
                }
            }
            throw dive;
        }

        if (recordatorioRepositorio != null) {
            try {
                recordatorioRepositorio.deleteByTurnoId(guardado.getId());
                recordatorioRepositorio.save(new RecordatorioTurno(guardado));
            } catch (Exception ex) {
                log.error("Fallo al crear recordatorio para el sobreturno ID={}: {}", guardado.getId(), ex.getMessage());
            }
        }

        if (notificacionUsuarioServicio != null && guardado.getPaciente() != null && guardado.getPaciente().getUsuario() != null) {
            notificacionUsuarioServicio.crearNotificacion(
                guardado.getPaciente().getUsuario(),
                "Sobreturno Solicitado",
                "Su sobreturno mÃƒÂ©dico de urgencia para el " + guardado.getFechaHora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " fue registrado exitosamente.",
                "TURNO",
                "/mis-turnos"
            );
        }

        return turnoMapper.paraDto(guardado);
    }

    @Override

    @Transactional(readOnly = true)

    public List<LocalDateTime> obtenerHorariosDisponibles(Long profesionalId, LocalDate fecha) {

        if (!profesionalRepositorio.existsById(profesionalId)) {

            throw new NoEncontradoExcepcion("Profesional no encontrado con ID: " + profesionalId);

        }

        if (fecha.isBefore(hoy())) {
            return List.of();
        }
        if (feriadoServicio != null) {
            if (!feriadoServicio.esDiaHabil(fecha)) {
                return List.of();
            }
        } else if (fecha.getDayOfWeek().getValue() > 5) {
            return List.of();
        }

        LocalDateTime inicio = fecha.atTime(apertura);
        LocalDateTime fin = fecha.atTime(cierre);

        List<LocalDateTime> ocupados = turnoRepositorio
                .findByProfesionalIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
                        profesionalId, inicio, fin, CANCELADO)
                .stream()
                .filter(t -> !"DISPONIBLE".equalsIgnoreCase(t.getEstado()))
                .map(Turno::getFechaHora)
                .collect(Collectors.toList());

        LocalDateTime ahora = ahora();
        List<LocalDateTime> disponibles = new ArrayList<>();

        List<DisponibilidadProfesional> disponibilidades = (disponibilidadRepositorio != null)
                ? disponibilidadRepositorio.findByProfesionalIdAndDiaSemanaAndActivoTrue(profesionalId, fecha.getDayOfWeek())
                : List.of();

        if (disponibilidadRepositorio != null) {
            // Regla explÃƒÂ­cita y segura: si no posee franjas configuradas para este día de la semana, la agenda se considera cerrada.
            if (disponibilidades.isEmpty()) {
                return List.of();
            }
            for (DisponibilidadProfesional disp : disponibilidades) {
                int intervalo = (disp.getIntervaloMinutos() != null && disp.getIntervaloMinutos() > 0)
                        ? disp.getIntervaloMinutos() : duracionSlotMinutos;
                LocalTime cursor = disp.getHoraInicio();
                while (cursor.plusMinutes(intervalo).compareTo(disp.getHoraFin()) <= 0) {
                    LocalDateTime slot = fecha.atTime(cursor);
                    if (slot.isAfter(ahora) && !ocupados.contains(slot)) {
                        disponibles.add(slot);
                    }
                    cursor = cursor.plusMinutes(intervalo);
                }
            }
        } else {
            for (LocalDateTime slot = inicio; slot.plusMinutes(duracionSlotMinutos).compareTo(fin) <= 0;
                 slot = slot.plusMinutes(duracionSlotMinutos)) {
                if (slot.isAfter(ahora) && !ocupados.contains(slot)) {
                    disponibles.add(slot);
                }
            }
        }
        disponibles.sort(LocalDateTime::compareTo);

        if (bloqueoAgendaRepositorio != null) {
            List<BloqueoAgenda> bloqueos = bloqueoAgendaRepositorio.findBloqueosEnRango(profesionalId, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
            if (!bloqueos.isEmpty()) {
                disponibles.removeIf(slot -> bloqueos.stream().anyMatch(b -> !slot.isBefore(b.getFechaInicio()) && slot.isBefore(b.getFechaFin())));
            }
        }

        return disponibles;

    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(this.clock.withZone(this.zoneId));
    }

    private LocalDate hoy() {
        return LocalDate.now(this.clock.withZone(this.zoneId));
    }

    private void validarHorario(LocalDateTime fechaHora) {

        LocalDateTime ahora = ahora();

        boolean diaHabil = feriadoServicio != null ? feriadoServicio.esDiaHabil(fechaHora.toLocalDate()) : (fechaHora.getDayOfWeek().getValue() <= 5);

        long minutosDesdeApertura = java.time.Duration.between(apertura, fechaHora.toLocalTime()).toMinutes();

        boolean dentroDeJornada = !fechaHora.toLocalTime().isBefore(apertura)

                && !fechaHora.toLocalTime().plusMinutes(duracionSlotMinutos).isAfter(cierre)

                && minutosDesdeApertura % duracionSlotMinutos == 0

                && fechaHora.getSecond() == 0 && fechaHora.getNano() == 0;

        if (!fechaHora.isAfter(ahora) || !diaHabil || !dentroDeJornada) {

            throw new IllegalArgumentException("El turno debe ser futuro, en día hÃƒÂ¡bil y respetar la jornada y duraciÃƒÂ³n de slots configuradas.");

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
    @Transactional(readOnly = true)
    public Page<TurnoDtoResponse> listarPaginados(Pageable pageable) {
        return turnoRepositorio.findAll(pageable).map(turnoMapper::paraDto);
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

    @Override

    @Transactional(rollbackFor = Exception.class)

    public TurnoDtoResponse cancelarTurno(Long id, String emailAutenticado) {

        Turno turno = turnoRepositorio.findById(id)

                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + id));

        if (emailAutenticado != null && !emailAutenticado.isBlank()) {

            Paciente paciente = pacienteRepositorio.findByUsuarioEmail(emailAutenticado).orElse(null);

            if (paciente != null && turno.getPaciente() != null && !turno.getPaciente().getId().equals(paciente.getId())) {

                throw new org.springframework.security.access.AccessDeniedException("No posee autorizaciÃƒÂ³n para cancelar el turno de otro paciente.");

            }

        }

        if ("CANCELADO".equalsIgnoreCase(turno.getEstado())) {

            throw new IllegalStateException("El turno ya se encuentra cancelado.");

        }

        if ("ATENDIDO".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalStateException("No se puede cancelar un turno que ya fue atendido.");
        }
        if ("AUSENTE".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalStateException("No se puede cancelar un turno que ya fue marcado como ausente.");
        }

        LocalDateTime ahora = ahora();

        if (turno.getFechaHora().isBefore(ahora.plusHours(2))) {

            throw new IllegalArgumentException("La cancelaciÃƒÂ³n debe realizarse con un mÃƒÂ­nimo de 2 horas de anticipaciÃƒÂ³n al horario del turno.");

        }

        turno.setEstado("CANCELADO");
        turno.setFechaCancelacion(ahora);
        turno.setMotivoCancelacion(emailAutenticado != null ? "Cancelado por usuario: " + emailAutenticado : "Cancelado por administracion");
        Turno guardado = turnoRepositorio.save(turno);
        if (recordatorioRepositorio != null) {
            try {
                recordatorioRepositorio.deleteByTurnoId(guardado.getId());
            } catch (Exception ex) {
                log.error("Fallo al eliminar recordatorio del turno cancelado ID={}: {}", guardado.getId(), ex.getMessage());
            }
        }

        if (notificacionUsuarioServicio != null && turno.getPaciente() != null && turno.getPaciente().getUsuario() != null) {
            notificacionUsuarioServicio.crearNotificacion(
                turno.getPaciente().getUsuario(),
                "Turno Cancelado",
                "Su turno del dia " + turno.getFechaHora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " ha sido cancelado.",
                "TURNO",
                "/mis-turnos"
            );
        }

        if (emailNotificacionServicio != null) {
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                emailNotificacionServicio.enviarCancelacionTurno(guardado, guardado.getMotivoCancelacion());
                            } catch (Exception ex) {
                                log.warn("Fallo al despachar email de cancelaciÃƒÂ³n post-commit para turno ID={}: {}", guardado.getId(), ex.getMessage());
                            }
                        }
                    }
                );
            } else {
                try {
                    emailNotificacionServicio.enviarCancelacionTurno(guardado, guardado.getMotivoCancelacion());
                } catch (Exception ex) {
                    log.warn("Fallo al despachar email de cancelaciÃƒÂ³n para turno ID={}: {}", guardado.getId(), ex.getMessage());
                }
            }
        }

        return turnoMapper.paraDto(guardado);

    }

    @Override
    @Transactional
    public TurnoDtoResponse confirmarTurno(Long id, String emailAutenticado) {
        Turno turno = turnoRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + id));

        if ("CANCELADO".equalsIgnoreCase(turno.getEstado()) || "ATENDIDO".equalsIgnoreCase(turno.getEstado()) || "AUSENTE".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalStateException("No se puede confirmar un turno en estado terminal (" + turno.getEstado() + ").");
        }
        if (!"PENDIENTE".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalStateException("Solo se pueden confirmar turnos en estado PENDIENTE. Estado actual: " + turno.getEstado());
        }

        turno.setEstado("CONFIRMADO");
        Turno guardado = turnoRepositorio.save(turno);

        if (notificacionUsuarioServicio != null && guardado.getPaciente() != null && guardado.getPaciente().getUsuario() != null) {
            notificacionUsuarioServicio.crearNotificacion(
                guardado.getPaciente().getUsuario(),
                "Turno Confirmado",
                "Su turno mÃƒÂ©dico para el " + guardado.getFechaHora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " ha sido confirmado.",
                "TURNO",
                "/mis-turnos"
            );
        }

        if (emailNotificacionServicio != null) {
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                emailNotificacionServicio.enviarConfirmacionTurno(guardado);
                            } catch (Exception ex) {
                                log.warn("Fallo al despachar email de confirmaciÃ³n post-commit para turno ID={}: {}", guardado.getId(), ex.getMessage());
                            }
                        }
                    }
                );
            } else {
                try {
                    emailNotificacionServicio.enviarConfirmacionTurno(guardado);
                } catch (Exception ex) {
                    log.warn("Fallo al despachar email de confirmaciÃƒÂ³n para turno ID={}: {}", guardado.getId(), ex.getMessage());
                }
            }
        }

        return turnoMapper.paraDto(guardado);
    }

    @Override

    @Transactional(rollbackFor = Exception.class)

    public TurnoDtoResponse reprogramarTurno(Long id, LocalDateTime nuevaFechaHora, String emailAutenticado) {

        Turno turno = turnoRepositorio.findById(id)

                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + id));

        if (emailAutenticado != null && !emailAutenticado.isBlank()) {

            Paciente paciente = pacienteRepositorio.findByUsuarioEmail(emailAutenticado).orElse(null);

            if (paciente != null && turno.getPaciente() != null && !turno.getPaciente().getId().equals(paciente.getId())) {

                throw new org.springframework.security.access.AccessDeniedException("No posee autorizaciÃƒÂ³n para reprogramar el turno de otro paciente.");

            }

        }

        if ("CANCELADO".equalsIgnoreCase(turno.getEstado()) || "ATENDIDO".equalsIgnoreCase(turno.getEstado()) || "AUSENTE".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalStateException("No se puede reprogramar un turno en estado terminal (" + turno.getEstado() + ").");
        }

        validarHorario(nuevaFechaHora);
        if (bloqueoAgendaRepositorio != null && turno.getProfesional() != null && !bloqueoAgendaRepositorio.findBloqueosParaFechaHora(turno.getProfesional().getId(), nuevaFechaHora).isEmpty()) {
            throw new ReglaNegocioException("El profesional seleccionado no se encuentra disponible debido a un bloqueo de agenda activo (licencia o ausencia programada).");
        }

        if (disponibilidadRepositorio != null && turno.getProfesional() != null) {
            List<DisponibilidadProfesional> disponibilidades = disponibilidadRepositorio.findByProfesionalIdAndDiaSemanaAndActivoTrue(turno.getProfesional().getId(), nuevaFechaHora.getDayOfWeek());
            if (disponibilidades.isEmpty()) {
                throw new ReglaNegocioException("El profesional no posee franjas de disponibilidad configuradas para el nuevo día seleccionado (agenda cerrada).");
            }
            boolean calza = disponibilidades.stream().anyMatch(disp -> {
                LocalTime t = nuevaFechaHora.toLocalTime();
                int dur = (disp.getIntervaloMinutos() != null && disp.getIntervaloMinutos() > 0) ? disp.getIntervaloMinutos() : duracionSlotMinutos;
                if (t.isBefore(disp.getHoraInicio()) || t.plusMinutes(dur).isAfter(disp.getHoraFin())) {
                    return false;
                }
                long mins = java.time.Duration.between(disp.getHoraInicio(), t).toMinutes();
                return mins % dur == 0;
            });
            if (!calza) {
                throw new ReglaNegocioException("El nuevo horario solicitado no coincide con la agenda configurada para el profesional.");
            }
        }

        if (turnoRepositorio.existsByProfesionalIdAndFechaHoraAndEstadoNot(turno.getProfesional().getId(), nuevaFechaHora, CANCELADO)) {
            throw new IllegalStateException("El nuevo horario solicitado ya no está disponible.");
        }

        if (turno.getPaciente() != null && turnoRepositorio.existsByPacienteIdAndFechaHoraAndEstadoNot(turno.getPaciente().getId(), nuevaFechaHora, CANCELADO)) {
            throw new ReglaNegocioException("El paciente ya cuenta con otro turno registrado en el mismo día y horario.");
        }

        turno.setFechaHora(nuevaFechaHora);
        turno.setEstado("PENDIENTE");

        Turno guardado = turnoRepositorio.save(turno);

        if (recordatorioRepositorio != null) {
            try {
                recordatorioRepositorio.deleteByTurnoId(guardado.getId());
                recordatorioRepositorio.save(new RecordatorioTurno(guardado));
            } catch (Exception ex) {
                log.error("Fallo al actualizar recordatorio para el turno reprogramado ID={}: {}", guardado.getId(), ex.getMessage());
            }
        }
        if (notificacionUsuarioServicio != null && guardado.getPaciente() != null && guardado.getPaciente().getUsuario() != null) {
            notificacionUsuarioServicio.crearNotificacion(
                guardado.getPaciente().getUsuario(),
                "Turno Confirmado",
                "Su turno mÃƒÂ©dico ha sido confirmado para el " + guardado.getFechaHora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                "TURNO",
                "/mis-turnos"
            );
        }

        return turnoMapper.paraDto(guardado);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TurnoDtoResponse realizarCheckIn(Long turnoId, String emailRecepcionista) {
        Turno turno = turnoRepositorio.findById(turnoId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + turnoId));

        TurnoEstado estadoActual = TurnoEstado.desdeString(turno.getEstado());
        if (!estadoActual.puedeTransicionarA(TurnoEstado.EN_SALA_DE_ESPERA)) {
            if ("CANCELADO".equalsIgnoreCase(turno.getEstado())) {
                throw new IllegalStateException("No se puede realizar el check-in de un turno cancelado.");
            }
            if ("ATENDIDO".equalsIgnoreCase(turno.getEstado())) {
                throw new IllegalStateException("El turno ya fue atendido previamente.");
            }
            if ("EN_SALA_DE_ESPERA".equalsIgnoreCase(turno.getEstado())) {
                throw new IllegalStateException("El paciente ya realizÃƒÂ³ el check-in y se encuentra en sala de espera.");
            }
            throw new ReglaNegocioException("Transicion de estado invalida para check-in.");
        }

        turno.setEstado("EN_SALA_DE_ESPERA");
        turno.setFechaCheckIn(ahora());
        turno.setRecepcionistaCheckIn(emailRecepcionista != null && !emailRecepcionista.isBlank() ? emailRecepcionista : "RECEPCION");

        Turno guardado = turnoRepositorio.save(turno);
        return turnoMapper.paraDto(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TurnoDtoResponse registrarCopago(Long turnoId, CopagoDtoRequest copagoDto) {
        Turno turno = turnoRepositorio.findById(turnoId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + turnoId));

        if ("CANCELADO".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalStateException("No se puede registrar copago para un turno cancelado.");
        }

        turno.setMontoCopago(copagoDto.getMonto());
        turno.setNumeroBono(copagoDto.getNumeroBono());
        turno.setMetodoPago(copagoDto.getMetodoPago() != null ? copagoDto.getMetodoPago().trim().toUpperCase() : null);
        turno.setFechaCopago(ahora());

        Turno guardado = turnoRepositorio.save(turno);
        return turnoMapper.paraDto(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TurnoDtoResponse atenderTurno(Long turnoId, AtencionTurnoDtoRequest atencionDto, String emailProfesional) {
        Turno turno = turnoRepositorio.findById(turnoId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + turnoId));

        if ("CANCELADO".equalsIgnoreCase(turno.getEstado()) || "AUSENTE".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalStateException("No se puede atender un turno en estado terminal (" + turno.getEstado() + ").");
        }
        if ("ATENDIDO".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalStateException("El turno ya fue atendido previamente.");
        }
        if (turno.getPaciente() == null) {
            throw new ReglaNegocioException("El turno no posee un paciente asociado para ser atendido.");
        }
        if (turno.getProfesional() == null) {
            throw new ReglaNegocioException("El turno no posee un profesional asignado.");
        }

        
        if (emailProfesional != null && !emailProfesional.isBlank()) {
            if (turno.getProfesional() != null && turno.getProfesional().getUsuario() != null) {
                if (!turno.getProfesional().getUsuario().getEmail().equalsIgnoreCase(emailProfesional)) {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    if (!isAdmin) {
                        throw new AccessDeniedException("No posee autorizaciÃƒÂ³n para atender un turno asignado a otro profesional.");
                    }
                }
            }
        }
        turno.setEstado("ATENDIDO");
        turno.setObservacionesAtencion(atencionDto.getObservaciones());
        turno.setFechaAtencion(ahora());

        Turno guardado = turnoRepositorio.save(turno);
        return turnoMapper.paraDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarCertificadoAsistencia(Long turnoId, String emailAutenticado) {
        Turno turno = turnoRepositorio.findById(turnoId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + turnoId));

        if (!"ATENDIDO".equalsIgnoreCase(turno.getEstado()) && !"EN_SALA_DE_ESPERA".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalStateException("Solo se puede emitir certificado de asistencia para turnos que han sido atendidos o se encuentran en sala de espera.");
        }

        if (emailAutenticado != null && !emailAutenticado.isBlank()) {
            Paciente paciente = pacienteRepositorio.findByUsuarioEmail(emailAutenticado).orElse(null);
            if (paciente != null && turno.getPaciente() != null && !turno.getPaciente().getId().equals(paciente.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("No posee autorizaciÃƒÂ³n para descargar certificados de otro paciente.");
            }
        }

        if (documentoPdfServicio == null) {
            throw new IllegalStateException("Servicio de generaciÃƒÂ³n PDF no disponible.");
        }

        String baseFrontend = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl.replaceAll("/+$", "") : "http://localhost:5173";
        return documentoPdfServicio.generarCertificadoAsistenciaPdf(turno, baseFrontend + "/validar-documento?codigo=" + turno.getId());
    }
    @Override
    @Transactional(readOnly = true)
    public List<TurnoDtoResponse> obtenerTurnosProximosPaciente(Long pacienteId) {
        LocalDateTime ahora = ahora();
        return turnoRepositorio.findByPacienteIdAndFechaHoraGreaterThanEqualOrderByFechaHoraAsc(pacienteId, ahora)
                .stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoDtoResponse> obtenerHistorialTurnosPaciente(Long pacienteId) {
        LocalDateTime ahora = ahora();
        return turnoRepositorio.findByPacienteIdAndFechaHoraLessThanOrderByFechaHoraDesc(pacienteId, ahora)
                .stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional
    public TurnoDtoResponse marcarAusente(Long turnoId, String emailPersonal) {
        Turno turno = turnoRepositorio.findById(turnoId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + turnoId));
        if ("ATENDIDO".equalsIgnoreCase(turno.getEstado()) || "FINALIZADO".equalsIgnoreCase(turno.getEstado()) || "CANCELADO".equalsIgnoreCase(turno.getEstado()) || "AUSENTE".equalsIgnoreCase(turno.getEstado())) {
            throw new ReglaNegocioException("No se puede marcar como ausente un turno en estado terminal: " + turno.getEstado());
        }
        if (turno.getFechaHora().isAfter(ahora())) {
            throw new ReglaNegocioException("No se puede marcar como ausente un turno cuyo horario aun no ha transcurrido.");
        }
        turno.setEstado("AUSENTE");
        Turno guardado = turnoRepositorio.save(turno);
        return turnoMapper.paraDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoDtoResponse> obtenerTurnosHoyProfesional(Long profesionalId) {
        LocalDate hoy = hoy();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.atTime(LocalTime.MAX);
        return turnoRepositorio.findByProfesionalIdAndFechaHoraBetween(profesionalId, inicio, fin)
                .stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }

    private String resolverNombreCentro(Long centroId) {
        if (centroId == null) return "Hospital Regional Aurelio Crespo";
        if (centroId == 1L) return "Hospital Regional Aurelio Crespo";
        if (centroId == 2L) return "Clínica Privada Cabrera";
        if (centroId == 3L) return "Centro de Diagnóstico Cruz del Eje";
        if (centroId == 4L) return "Dispensario Municipal Santa Rosa";
        return "Centro de Salud N° " + centroId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoDtoResponse> obtenerAgendaProfesional(Long profesionalId, Long centroSaludId, LocalDate fecha) {
        if (!profesionalRepositorio.existsById(profesionalId)) {
            throw new NoEncontradoExcepcion("Profesional no encontrado con ID: " + profesionalId);
        }
        LocalDateTime inicio = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime fin = fecha != null ? fecha.atTime(LocalTime.MAX) : null;
        return turnoRepositorio.findAgendaProfesional(profesionalId, centroSaludId, inicio, fin)
                .stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoDtoResponse> obtenerMiAgenda(String emailProfesional, Long centroSaludId, LocalDate fecha) {
        Profesional profesional = profesionalRepositorio.findByUsuarioEmail(emailProfesional)
                .orElseThrow(() -> new NoEncontradoExcepcion("No se encontró perfil profesional para el usuario: " + emailProfesional));
        return obtenerAgendaProfesional(profesional.getId(), centroSaludId, fecha);
    }


    private void validarCoincidenciaYAutorizacionIdempotencia(Turno previo, Long pacienteId, Long profesionalId, LocalDateTime fechaHora, boolean esSobreturno, String emailAutenticado, String key) {
        if (emailAutenticado != null && !emailAutenticado.isBlank()) {
            boolean esDuenoPaciente = previo.getPaciente() != null && previo.getPaciente().getUsuario() != null
                    && emailAutenticado.equalsIgnoreCase(previo.getPaciente().getUsuario().getEmail());
            boolean esDuenoMedico = previo.getProfesional() != null && previo.getProfesional().getUsuario() != null
                    && emailAutenticado.equalsIgnoreCase(previo.getProfesional().getUsuario().getEmail());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean esAdminORecepcion = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_RECEPCIONISTA"));

            if (!esDuenoPaciente && !esDuenoMedico && !esAdminORecepcion) {
                log.warn("Intento de acceso no autorizado a turno preexistente vía idempotencyKey '{}' por usuario '{}'", key, MascaraDatosUtil.enmascararEmail(emailAutenticado));
                throw new AccessDeniedException("No está autorizado a consultar o recuperar el turno asociado a esta clave de idempotencia.");
            }
        }

        boolean previoEsSobreturno = Boolean.TRUE.equals(previo.getEsSobreturno());
        if (previoEsSobreturno != esSobreturno) {
            throw new IllegalStateException("Conflicto de idempotencia: La clave '" + key + "' fue utilizada previamente para un tipo de operación distinto.");
        }

        boolean coincidePaciente = (pacienteId != null && previo.getPaciente() != null && previo.getPaciente().getId().equals(pacienteId))
                || (pacienteId == null && previo.getPaciente() != null && previo.getPaciente().getUsuario() != null && emailAutenticado != null && emailAutenticado.equalsIgnoreCase(previo.getPaciente().getUsuario().getEmail()));
        boolean coincideProfesional = previo.getProfesional() != null && previo.getProfesional().getId().equals(profesionalId);
        boolean coincideFecha = previo.getFechaHora() != null && previo.getFechaHora().isEqual(fechaHora);

        if (!coincidePaciente || !coincideProfesional || !coincideFecha) {
            throw new IllegalStateException("Conflicto de idempotencia: La clave '" + key + "' ya fue utilizada previamente con parámetros de reserva diferentes.");
        }
    }
}
