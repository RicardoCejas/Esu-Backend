package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.OutboxEvento;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.OutboxEventoRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxEventoRepositorio outboxEventoRepositorio;
    private final RecetaDigitalRepositorio recetaRepositorio;
    private final TurnoRepositorio turnoRepositorio;
    private final DocumentoPdfServicio pdfServicio;
    private final EmailNotificacionServicio emailServicio;
    private final Clock clock;
    private final ZoneId turnosZoneId;

    private LocalDateTime ahora() {
        return LocalDateTime.now(this.clock.withZone(this.turnosZoneId));
    }

    /**
     * Tarea programada que ejecuta el ciclo de Outbox SIN mantener una transacción abierta
     * durante la generación de PDFs o llamadas de red SMTP.
     */
    @Scheduled(fixedDelayString = "${esu.outbox.fixed-delay-ms:10000}")
    public void procesarEventosPendientes() {
        List<OutboxEvento> claimed = reclamarEventosParaProcesar(20);
        if (claimed.isEmpty()) {
            return;
        }

        log.debug("Procesando {} eventos de outbox reclamados de forma atómica", claimed.size());
        for (OutboxEvento ev : claimed) {
            procesarEventoIndividual(ev);
        }
    }

    /**
     * Paso 1: Reclamar lote con bloqueo pesimista y asignar lease/estado 'EN_PROCESO'
     * en una transacción muy breve (milisegundos).
     */
    @Transactional
    public List<OutboxEvento> reclamarEventosParaProcesar(int batchSize) {
        LocalDateTime ahora = ahora();
        List<OutboxEvento> eventos = outboxEventoRepositorio.reclamarEventosDisponibles(ahora, PageRequest.of(0, batchSize));
        for (OutboxEvento ev : eventos) {
            ev.setEstado("EN_PROCESO");
            // Lease de 2 minutos para procesamiento seguro antes de considerarse abandonado
            ev.setLeaseHasta(ahora.plusMinutes(2));
            int intentos = ev.getIntentos() != null ? ev.getIntentos() + 1 : 1;
            ev.setIntentos(intentos);
            ev.setFechaActualizacion(ahora);
        }
        return outboxEventoRepositorio.saveAll(eventos);
    }

    /**
     * Paso 2: Ejecución individual fuera de transacción global para no bloquear la BD
     */
    public void procesarEventoIndividual(OutboxEvento ev) {
        try {
            // Idempotencia externa: si el efecto ya fue emitido y solo faltaba confirmar en BD
            if (Boolean.TRUE.equals(ev.getDespachadoExterno())) {
                log.info("Evento outbox ID={} ya fue despachado externamente. Omitiendo reenvío de correo y confirmando estado ENVIADO.", ev.getId());
                marcarExitoTransaccional(ev.getId());
                return;
            }

            if ("RECETA_EMITIDA".equalsIgnoreCase(ev.getTipoEvento())) {
                if (recetaRepositorio == null || pdfServicio == null || emailServicio == null) {
                    marcarFalloTransaccional(ev.getId(), "Servicios requeridos no disponibles", ev.getIntentos());
                    return;
                }

                RecetaDigital receta = recetaRepositorio.findById(ev.getAggregateId()).orElse(null);
                if (receta == null) {
                    marcarInconsistenteTransaccional(ev.getId(), "Receta digital no encontrada con ID: " + ev.getAggregateId());
                    return;
                }

                // Generación de PDF y envío SMTP fuera de transacción de base de datos
                byte[] pdfBytes = pdfServicio.generarRecetaPdf(receta, ev.getPayload());
                emailServicio.enviarRecetaDigital(receta, pdfBytes);
                marcarDespachoExterno(ev.getId());

                marcarExitoTransaccional(ev.getId());

            } else if ("TURNO_CREADO".equalsIgnoreCase(ev.getTipoEvento())) {
                if (turnoRepositorio == null || emailServicio == null) {
                    marcarFalloTransaccional(ev.getId(), "Servicios requeridos no disponibles", ev.getIntentos());
                    return;
                }

                Turno turno = turnoRepositorio.findById(ev.getAggregateId()).orElse(null);
                if (turno == null) {
                    marcarInconsistenteTransaccional(ev.getId(), "Turno no encontrado con ID: " + ev.getAggregateId());
                    return;
                }

                // Despacho de email fuera de transacción de base de datos
                emailServicio.enviarConfirmacionTurno(turno);
                marcarDespachoExterno(ev.getId());

                marcarExitoTransaccional(ev.getId());

            } else {
                marcarErrorTipoDesconocidoTransaccional(ev.getId(), "Tipo de evento no soportado por outbox: " + ev.getTipoEvento());
            }

        } catch (Exception ex) {
            log.error("Fallo al despachar outbox evento ID={}: {}", ev.getId(), ex.getMessage());
            marcarFalloTransaccional(ev.getId(), ex.getMessage(), ev.getIntentos());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarDespachoExterno(Long eventoId) {
        outboxEventoRepositorio.findById(eventoId).ifPresent(e -> {
            e.setDespachadoExterno(true);
            e.setFechaActualizacion(ahora());
            outboxEventoRepositorio.save(e);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarExitoTransaccional(Long eventoId) {
        outboxEventoRepositorio.findById(eventoId).ifPresent(e -> {
            e.setEstado("ENVIADO");
            e.setDespachadoExterno(true);
            e.setLeaseHasta(null);
            e.setProximoIntento(null);
            e.setFechaActualizacion(ahora());
            outboxEventoRepositorio.save(e);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarInconsistenteTransaccional(Long eventoId, String motivo) {
        outboxEventoRepositorio.findById(eventoId).ifPresent(e -> {
            log.error("Inconsistencia en outbox evento ID={}: {}", eventoId, motivo);
            e.setEstado("INCONSISTENTE_NO_ENCONTRADO");
            e.setUltimoError(motivo);
            e.setLeaseHasta(null);
            e.setProximoIntento(null);
            e.setFechaActualizacion(ahora());
            outboxEventoRepositorio.save(e);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarErrorTipoDesconocidoTransaccional(Long eventoId, String motivo) {
        outboxEventoRepositorio.findById(eventoId).ifPresent(e -> {
            log.error("Tipo desconocido en outbox evento ID={}: {}", eventoId, motivo);
            e.setEstado("ERROR_TIPO_DESCONOCIDO");
            e.setUltimoError(motivo);
            e.setLeaseHasta(null);
            e.setProximoIntento(null);
            e.setFechaActualizacion(ahora());
            outboxEventoRepositorio.save(e);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFalloTransaccional(Long eventoId, String error, int intentos) {
        outboxEventoRepositorio.findById(eventoId).ifPresent(e -> {
            e.setUltimoError(error);
            e.setLeaseHasta(null);
            LocalDateTime ahora = ahora();
            e.setFechaActualizacion(ahora);

            if (intentos >= 3) {
                e.setEstado("FALLIDO");
                e.setProximoIntento(null);
            } else {
                long backoffSegundos = (long) Math.min(300, Math.pow(2, intentos) * 10);
                e.setEstado("PENDIENTE");
                e.setProximoIntento(ahora.plusSeconds(backoffSegundos));
            }
            outboxEventoRepositorio.save(e);
        });
    }

    /**
     * Mecanismo de reintento manual seguro invocado por administradores.
     */
    @Transactional
    public boolean reintentarEventoManual(Long eventoId) {
        OutboxEvento ev = outboxEventoRepositorio.findById(eventoId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Evento outbox no encontrado con ID: " + eventoId));
        ev.setEstado("PENDIENTE");
        ev.setIntentos(0);
        ev.setLeaseHasta(null);
        ev.setProximoIntento(null);
        ev.setUltimoError(null);
        ev.setFechaActualizacion(ahora());
        outboxEventoRepositorio.save(ev);
        return true;
    }

    @Transactional(readOnly = true)
    public List<OutboxEvento> listarEventosConProblemas(PageRequest pageRequest) {
        return outboxEventoRepositorio.findByEstadoInOrderByFechaCreacionDesc(
                List.of("FALLIDO", "ERROR_TIPO_DESCONOCIDO", "INCONSISTENTE_NO_ENCONTRADO"),
                pageRequest
        );
    }
}
