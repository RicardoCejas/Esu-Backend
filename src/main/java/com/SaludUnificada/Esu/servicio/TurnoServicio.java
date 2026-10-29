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
import com.SaludUnificada.Esu.entidad.BloqueoAgenda;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
        LocalDateTime ahora = LocalDateTime.now(this.clock);
        if (fechaHora.isBefore(ahora)) {
            throw new ReglaNegocioException("No es posible solicitar turnos medicos en fechas u horarios pasados.");
        }

        Profesional profesional = profesionalRepositorio.findByIdForUpdate(turnoDto.getProfesionalId())
                .or(() -> profesionalRepositorio.findById(turnoDto.getProfesionalId()))

                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + turnoDto.getProfesionalId()));
        if (bloqueoAgendaRepositorio != null && !bloqueoAgendaRepositorio.findBloqueosParaFechaHora(profesional.getId(), fechaHora).isEmpty()) {
            throw new ReglaNegocioException("El profesional seleccionado no se encuentra disponible debido a un bloqueo de agenda activo (licencia o ausencia programada).");
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

        Turno turno = new Turno();

        turno.setFechaHora(fechaHora);

        turno.setMotivoTurno(turnoDto.getMotivoTurno());

        turno.setEstado("PENDIENTE");

        turno.setPaciente(paciente);

        turno.setProfesional(profesional);

        Turno guardado = turnoRepositorio.save(turno);

        recordatorioRepositorio.save(new RecordatorioTurno(guardado));
        if (notificacionUsuarioServicio != null && guardado.getPaciente() != null && guardado.getPaciente().getUsuario() != null) {
            notificacionUsuarioServicio.crearNotificacion(
                guardado.getPaciente().getUsuario(),
                "Turno Solicitado",
                "Su turno médico para el " + guardado.getFechaHora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " fue registrado y se encuentra pendiente de confirmación.",
                "TURNO",
                "/mis-turnos"
            );
        }

        return turnoMapper.paraDto(guardado);

    }

    @Override

    @Transactional

    public TurnoDtoResponse crearSobreturno(SobreturnoDtoRequest request) {
        LocalDateTime ahora = LocalDateTime.now(this.clock);
        if (request.getFechaHora().isBefore(ahora)) {
            throw new ReglaNegocioException("No es posible solicitar sobreturnos en fechas u horarios pasados.");
        }

        Profesional profesional = profesionalRepositorio.findByIdForUpdate(request.getProfesionalId())
                .or(() -> profesionalRepositorio.findById(request.getProfesionalId()))
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + request.getProfesionalId()));

        if (bloqueoAgendaRepositorio != null && !bloqueoAgendaRepositorio.findBloqueosParaFechaHora(profesional.getId(), request.getFechaHora()).isEmpty()) {
            throw new ReglaNegocioException("El profesional seleccionado no se encuentra disponible debido a un bloqueo de agenda activo (licencia o ausencia programada).");
        }

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

                .stream()

                .filter(t -> !"DISPONIBLE".equalsIgnoreCase(t.getEstado()))

                .map(Turno::getFechaHora)

                .collect(Collectors.toList());

        LocalDateTime ahora = LocalDateTime.now(clock.withZone(zoneId));

        List<LocalDateTime> disponibles = new ArrayList<>();

        for (LocalDateTime slot = inicio; slot.plusMinutes(duracionSlotMinutos).compareTo(fin) <= 0;

             slot = slot.plusMinutes(duracionSlotMinutos)) {

            if (slot.isAfter(ahora) && !ocupados.contains(slot)) {

                disponibles.add(slot);

            }

        }

        if (bloqueoAgendaRepositorio != null) {
            List<BloqueoAgenda> bloqueos = bloqueoAgendaRepositorio.findBloqueosEnRango(profesionalId, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
            if (!bloqueos.isEmpty()) {
                disponibles.removeIf(slot -> bloqueos.stream().anyMatch(b -> !slot.isBefore(b.getFechaInicio()) && slot.isBefore(b.getFechaFin())));
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

                throw new org.springframework.security.access.AccessDeniedException("No posee autorización para cancelar el turno de otro paciente.");

            }

        }

        if ("CANCELADO".equalsIgnoreCase(turno.getEstado())) {

            throw new IllegalStateException("El turno ya se encuentra cancelado.");

        }

        if ("ATENDIDO".equalsIgnoreCase(turno.getEstado())) {

            throw new IllegalStateException("No se puede cancelar un turno que ya fue atendido.");

        }

        LocalDateTime ahora = LocalDateTime.now(clock.withZone(zoneId));

        if (turno.getFechaHora().isBefore(ahora.plusHours(2))) {

            throw new IllegalArgumentException("La cancelación debe realizarse con un mínimo de 2 horas de anticipación al horario del turno.");

        }

        turno.setEstado("CANCELADO");
        turno.setFechaCancelacion(ahora);
        turno.setMotivoCancelacion(emailAutenticado != null ? "Cancelado por usuario: " + emailAutenticado : "Cancelado por administracion");
        Turno guardado = turnoRepositorio.save(turno);
        try {
            recordatorioRepositorio.deleteByTurnoId(guardado.getId());
        } catch (Exception ignored) {}

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
                            } catch (Exception ignored) {}
                        }
                    }
                );
            } else {
                try { emailNotificacionServicio.enviarCancelacionTurno(guardado, guardado.getMotivoCancelacion()); } catch (Exception ignored) {}
            }
        }

        return turnoMapper.paraDto(guardado);

    }

    @Override
    @Transactional
    public TurnoDtoResponse confirmarTurno(Long id, String emailAutenticado) {
        Turno turno = turnoRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + id));

        if (!"PENDIENTE".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalStateException("Solo se pueden confirmar turnos en estado PENDIENTE. Estado actual: " + turno.getEstado());
        }

        turno.setEstado("CONFIRMADO");
        Turno guardado = turnoRepositorio.save(turno);

        if (notificacionUsuarioServicio != null && guardado.getPaciente() != null && guardado.getPaciente().getUsuario() != null) {
            notificacionUsuarioServicio.crearNotificacion(
                guardado.getPaciente().getUsuario(),
                "Turno Confirmado",
                "Su turno médico para el " + guardado.getFechaHora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " ha sido confirmado.",
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
                            } catch (Exception ignored) {}
                        }
                    }
                );
            } else {
                try { emailNotificacionServicio.enviarConfirmacionTurno(guardado); } catch (Exception ignored) {}
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

                throw new org.springframework.security.access.AccessDeniedException("No posee autorización para reprogramar el turno de otro paciente.");

            }

        }

        if ("CANCELADO".equalsIgnoreCase(turno.getEstado()) || "ATENDIDO".equalsIgnoreCase(turno.getEstado())) {

            throw new IllegalStateException("No se puede reprogramar un turno que ya fue atendido o cancelado.");

        }

        validarHorario(nuevaFechaHora);
        if (bloqueoAgendaRepositorio != null && turno.getProfesional() != null && !bloqueoAgendaRepositorio.findBloqueosParaFechaHora(turno.getProfesional().getId(), nuevaFechaHora).isEmpty()) {
            throw new ReglaNegocioException("El profesional seleccionado no se encuentra disponible debido a un bloqueo de agenda activo (licencia o ausencia programada).");
        }

        if (turnoRepositorio.existsByProfesionalIdAndFechaHoraAndEstadoNot(turno.getProfesional().getId(), nuevaFechaHora, CANCELADO)) {

            throw new IllegalStateException("El nuevo horario solicitado ya no está disponible.");

        }

        turno.setFechaHora(nuevaFechaHora);

        turno.setEstado("PENDIENTE");

        Turno guardado = turnoRepositorio.save(turno);

        recordatorioRepositorio.save(new RecordatorioTurno(guardado));
        if (notificacionUsuarioServicio != null && guardado.getPaciente() != null && guardado.getPaciente().getUsuario() != null) {
            notificacionUsuarioServicio.crearNotificacion(
                guardado.getPaciente().getUsuario(),
                "Turno Confirmado",
                "Su turno médico ha sido confirmado para el " + guardado.getFechaHora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
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
                throw new IllegalStateException("El paciente ya realizó el check-in y se encuentra en sala de espera.");
            }
            throw new ReglaNegocioException("Transicion de estado invalida para check-in.");
        }

        turno.setEstado("EN_SALA_DE_ESPERA");
        turno.setFechaCheckIn(LocalDateTime.now(clock.withZone(zoneId)));
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
        turno.setFechaCopago(LocalDateTime.now(clock.withZone(zoneId)));

        Turno guardado = turnoRepositorio.save(turno);
        return turnoMapper.paraDto(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TurnoDtoResponse atenderTurno(Long turnoId, AtencionTurnoDtoRequest atencionDto, String emailProfesional) {
        Turno turno = turnoRepositorio.findById(turnoId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + turnoId));

        if ("CANCELADO".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalStateException("No se puede atender un turno cancelado.");
        }

        
        if (emailProfesional != null && !emailProfesional.isBlank()) {
            if (turno.getProfesional() != null && turno.getProfesional().getUsuario() != null) {
                if (!turno.getProfesional().getUsuario().getEmail().equalsIgnoreCase(emailProfesional)) {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    if (!isAdmin) {
                        throw new AccessDeniedException("No posee autorización para atender un turno asignado a otro profesional.");
                    }
                }
            }
        }
        turno.setEstado("ATENDIDO");
        turno.setObservacionesAtencion(atencionDto.getObservaciones());
        turno.setFechaAtencion(LocalDateTime.now(clock.withZone(zoneId)));

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
                throw new org.springframework.security.access.AccessDeniedException("No posee autorización para descargar certificados de otro paciente.");
            }
        }

        if (documentoPdfServicio == null) {
            throw new IllegalStateException("Servicio de generación PDF no disponible.");
        }

        return documentoPdfServicio.generarCertificadoAsistenciaPdf(turno, "http://localhost:5173/validar-documento?codigo=" + turno.getId());
    }
    @Override
    @Transactional(readOnly = true)
    public List<TurnoDtoResponse> obtenerTurnosProximosPaciente(Long pacienteId) {
        LocalDateTime ahora = LocalDateTime.now(this.clock);
        return turnoRepositorio.findByPacienteIdAndFechaHoraGreaterThanEqualOrderByFechaHoraAsc(pacienteId, ahora)
                .stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoDtoResponse> obtenerHistorialTurnosPaciente(Long pacienteId) {
        LocalDateTime ahora = LocalDateTime.now(this.clock);
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
        if ("ATENDIDO".equalsIgnoreCase(turno.getEstado()) || "FINALIZADO".equalsIgnoreCase(turno.getEstado())) {
            throw new ReglaNegocioException("No se puede marcar como ausente un turno que ya ha sido atendido.");
        }
        turno.setEstado("AUSENTE");
        Turno guardado = turnoRepositorio.save(turno);
        return turnoMapper.paraDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoDtoResponse> obtenerTurnosHoyProfesional(Long profesionalId) {
        LocalDate hoy = LocalDate.now(this.clock);
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.atTime(LocalTime.MAX);
        return turnoRepositorio.findByProfesionalIdAndFechaHoraBetween(profesionalId, inicio, fin)
                .stream()
                .map(turnoMapper::paraDto)
                .collect(Collectors.toList());
    }
}
