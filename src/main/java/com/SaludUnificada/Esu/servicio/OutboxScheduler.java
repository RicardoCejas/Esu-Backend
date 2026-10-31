package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.OutboxEventoDtoResponse;
import com.SaludUnificada.Esu.entidad.OutboxEvento;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.repositorio.OutboxEventoRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
public class OutboxScheduler {

    private final OutboxEventoRepositorio outboxEventoRepositorio;
    private final RecetaDigitalRepositorio recetaRepositorio;
    private final TurnoRepositorio turnoRepositorio;
    private final DocumentoPdfServicio pdfServicio;
    private final EmailNotificacionServicio emailServicio;
    private final Clock clock;
    private final ZoneId turnosZoneId;
    private final OutboxTransaccionServicio outboxTransaccionServicio;

    @Autowired
    public OutboxScheduler(
            OutboxEventoRepositorio outboxEventoRepositorio,
            RecetaDigitalRepositorio recetaRepositorio,
            TurnoRepositorio turnoRepositorio,
            DocumentoPdfServicio pdfServicio,
            EmailNotificacionServicio emailServicio,
            Clock clock,
            ZoneId turnosZoneId,
            @Autowired(required = false) OutboxTransaccionServicio outboxTransaccionServicio
    ) {
        this.outboxEventoRepositorio = outboxEventoRepositorio;
        this.recetaRepositorio = recetaRepositorio;
        this.turnoRepositorio = turnoRepositorio;
        this.pdfServicio = pdfServicio;
        this.emailServicio = emailServicio;
        this.clock = clock;
        this.turnosZoneId = turnosZoneId;
        this.outboxTransaccionServicio = (outboxTransaccionServicio != null)
                ? outboxTransaccionServicio
                : new OutboxTransaccionServicio(outboxEventoRepositorio, clock, turnosZoneId);
    }

    public OutboxScheduler(
            OutboxEventoRepositorio outboxEventoRepositorio,
            RecetaDigitalRepositorio recetaRepositorio,
            TurnoRepositorio turnoRepositorio,
            DocumentoPdfServicio pdfServicio,
            EmailNotificacionServicio emailServicio,
            Clock clock,
            ZoneId turnosZoneId
    ) {
        this(outboxEventoRepositorio, recetaRepositorio, turnoRepositorio, pdfServicio, emailServicio, clock, turnosZoneId, null);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(this.clock.withZone(this.turnosZoneId));
    }

    /**
     * Tarea programada que ejecuta el ciclo de Outbox SIN mantener una transacciÃ³n abierta
     * durante la generaciÃ³n de PDFs o llamadas de red SMTP.
     */
    @Scheduled(fixedDelayString = "${esu.outbox.fixed-delay-ms:10000}")
    public void procesarEventosPendientes() {
        List<OutboxEvento> claimed = outboxTransaccionServicio.reclamarEventosParaProcesar(20);
        if (claimed.isEmpty()) {
            return;
        }

        log.debug("Procesando {} eventos de outbox reclamados de forma atÃ³mica", claimed.size());
        for (OutboxEvento ev : claimed) {
            procesarEventoIndividual(ev);
        }
    }

    /**
     * Procesamiento de evento individual:
     * - GeneraciÃ³n de PDF y envÃ­o de correo se ejecutan fuera de cualquier transacciÃ³n de BD.
     * - Las actualizaciones de estado y lease se realizan mediante OutboxTransaccionServicio en transacciones dedicadas.
     */
    public void procesarEventoIndividual(OutboxEvento ev) {
        try {
            // Idempotencia externa: si el efecto ya fue emitido y solo faltaba confirmar en BD
            if (Boolean.TRUE.equals(ev.getDespachadoExterno())) {
                log.info("Evento outbox ID={} ya fue despachado externamente. Omitiendo reenvÃ­o de correo y confirmando estado ENVIADO.", ev.getId());
                outboxTransaccionServicio.marcarExito(ev.getId());
                return;
            }

            if ("RECETA_EMITIDA".equalsIgnoreCase(ev.getTipoEvento())) {
                if (recetaRepositorio == null || pdfServicio == null || emailServicio == null) {
                    outboxTransaccionServicio.marcarFallo(ev.getId(), "Servicios requeridos no disponibles", ev.getIntentos());
                    return;
                }

                RecetaDigital receta = recetaRepositorio.findById(ev.getAggregateId()).orElse(null);
                if (receta == null) {
                    outboxTransaccionServicio.marcarInconsistente(ev.getId(), "Receta digital no encontrada con ID: " + ev.getAggregateId());
                    return;
                }

                // GeneraciÃ³n de PDF y envÃ­o SMTP fuera de transacciÃ³n de base de datos
                byte[] pdfBytes = pdfServicio.generarRecetaPdf(receta, ev.getPayload());
                emailServicio.enviarRecetaDigital(receta, pdfBytes);
                outboxTransaccionServicio.marcarDespachoExterno(ev.getId());
                outboxTransaccionServicio.marcarExito(ev.getId());

            } else if ("TURNO_CREADO".equalsIgnoreCase(ev.getTipoEvento())) {
                if (turnoRepositorio == null || emailServicio == null) {
                    outboxTransaccionServicio.marcarFallo(ev.getId(), "Servicios requeridos no disponibles", ev.getIntentos());
                    return;
                }

                Turno turno = turnoRepositorio.findById(ev.getAggregateId()).orElse(null);
                if (turno == null) {
                    outboxTransaccionServicio.marcarInconsistente(ev.getId(), "Turno no encontrado con ID: " + ev.getAggregateId());
                    return;
                }

                // Despacho de email fuera de transacciÃ³n de base de datos
                emailServicio.enviarConfirmacionTurno(turno);
                outboxTransaccionServicio.marcarDespachoExterno(ev.getId());
                outboxTransaccionServicio.marcarExito(ev.getId());

            } else {
                outboxTransaccionServicio.marcarErrorTipoDesconocido(ev.getId(), "Tipo de evento no soportado por outbox: " + ev.getTipoEvento());
            }

        } catch (Exception ex) {
            log.error("Fallo al despachar outbox evento ID={}: {}", ev.getId(), ex.getMessage());
            outboxTransaccionServicio.marcarFallo(ev.getId(), ex.getMessage(), ev.getIntentos());
        }
    }

    public List<OutboxEvento> reclamarEventosParaProcesar(int batchSize) {
        return outboxTransaccionServicio.reclamarEventosParaProcesar(batchSize);
    }

    public void marcarDespachoExterno(Long eventoId) {
        outboxTransaccionServicio.marcarDespachoExterno(eventoId);
    }

    public void marcarExitoTransaccional(Long eventoId) {
        outboxTransaccionServicio.marcarExito(eventoId);
    }

    public void marcarInconsistenteTransaccional(Long eventoId, String motivo) {
        outboxTransaccionServicio.marcarInconsistente(eventoId, motivo);
    }

    public void marcarErrorTipoDesconocidoTransaccional(Long eventoId, String motivo) {
        outboxTransaccionServicio.marcarErrorTipoDesconocido(eventoId, motivo);
    }

    public void marcarFalloTransaccional(Long eventoId, String error, int intentos) {
        outboxTransaccionServicio.marcarFallo(eventoId, error, intentos);
    }

    public boolean reintentarEventoManual(Long eventoId) {
        return outboxTransaccionServicio.reintentarEventoManual(eventoId);
    }

    public List<OutboxEventoDtoResponse> listarEventosConProblemas(PageRequest pageRequest) {
        return outboxTransaccionServicio.listarEventosConProblemas(pageRequest);
    }
}
