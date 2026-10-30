package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.OutboxEvento;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.OutboxEventoRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxTransaccionServicio {

    private final OutboxEventoRepositorio outboxEventoRepositorio;
    private final Clock clock;
    private final ZoneId turnosZoneId;

    public LocalDateTime ahora() {
        return LocalDateTime.now(this.clock.withZone(this.turnosZoneId));
    }

    /**
     * Reclama un lote de eventos con bloqueo pesimista y actualiza el lease_hasta en una transacción atómica breve.
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
     * Registra que el efecto externo (ej: email) ya fue despachado.
     * Ejecuta en transacción nueva e independiente (REQUIRES_NEW).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarDespachoExterno(Long eventoId) {
        outboxEventoRepositorio.findById(eventoId).ifPresent(e -> {
            e.setDespachadoExterno(true);
            e.setFechaActualizacion(ahora());
            outboxEventoRepositorio.save(e);
        });
    }

    /**
     * Confirma la entrega exitosa del evento en BD.
     * Ejecuta en transacción nueva e independiente (REQUIRES_NEW).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarExito(Long eventoId) {
        outboxEventoRepositorio.findById(eventoId).ifPresent(e -> {
            e.setEstado("ENVIADO");
            e.setDespachadoExterno(true);
            e.setLeaseHasta(null);
            e.setProximoIntento(null);
            e.setFechaActualizacion(ahora());
            outboxEventoRepositorio.save(e);
        });
    }

    /**
     * Marca un evento como inconsistente cuando el turno o receta referenciado no existe.
     * Nunca se marca como enviado.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarInconsistente(Long eventoId, String motivo) {
        outboxEventoRepositorio.findById(eventoId).ifPresent(e -> {
            log.error("Inconsistencia en outbox evento ID={}: {}", eventoId, motivo);
            e.setEstado("INCONSISTENTE");
            e.setUltimoError(motivo);
            e.setLeaseHasta(null);
            e.setProximoIntento(null);
            e.setFechaActualizacion(ahora());
            outboxEventoRepositorio.save(e);
        });
    }

    /**
     * Marca error ante un tipo de evento no reconocido por los despachadores.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarErrorTipoDesconocido(Long eventoId, String motivo) {
        outboxEventoRepositorio.findById(eventoId).ifPresent(e -> {
            log.error("Tipo desconocido en outbox evento ID={}: {}", eventoId, motivo);
            e.setEstado("TIPO_DESCONOCIDO");
            e.setUltimoError(motivo);
            e.setLeaseHasta(null);
            e.setProximoIntento(null);
            e.setFechaActualizacion(ahora());
            outboxEventoRepositorio.save(e);
        });
    }

    /**
     * Registra un fallo transitorio con backoff exponencial, o estado terminal FALLIDO tras 3 intentos.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFallo(Long eventoId, String error, int intentos) {
        outboxEventoRepositorio.findById(eventoId).ifPresent(e -> {
            e.setUltimoError(error);
            e.setLeaseHasta(null);
            LocalDateTime ahora = ahora();
            e.setFechaActualizacion(ahora);

            if (intentos >= 3) {
                e.setEstado("FALLIDO");
                e.setProximoIntento(null);
                log.error("Evento outbox ID={} superó el límite máximo de reintentos (3). Estado marcado como FALLIDO terminal.", eventoId);
            } else {
                long backoffSegundos = (long) Math.min(300, Math.pow(2, intentos) * 10);
                e.setEstado("PENDIENTE");
                e.setProximoIntento(ahora.plusSeconds(backoffSegundos));
                log.warn("Evento outbox ID={} falló (intento {}/3). Reprogramado con backoff en {} segundos.", eventoId, intentos, backoffSegundos);
            }
            outboxEventoRepositorio.save(e);
        });
    }

    /**
     * Reintento administrativo manual.
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
