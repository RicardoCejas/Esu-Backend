package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.OutboxEvento;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.repositorio.OutboxEventoRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final NotificacionUsuarioServicio notificacionUsuarioServicio;

    @Scheduled(fixedDelayString = "${esu.outbox.fixed-delay-ms:10000}")
    @Transactional
    public void procesarEventosPendientes() {
        List<OutboxEvento> eventos = outboxEventoRepositorio.findByEstadoForUpdate("PENDIENTE", PageRequest.of(0, 50));
        if (eventos.isEmpty()) {
            return;
        }

        log.debug("Procesando {} eventos de outbox pendientes", eventos.size());
        for (OutboxEvento ev : eventos) {
            try {
                despacharEvento(ev);
                ev.setEstado("ENVIADO");
                ev.setFechaActualizacion(LocalDateTime.now());
            } catch (IllegalArgumentException ex) {
                log.error("Tipo de evento no soportado para outbox evento ID={}: {}", ev.getId(), ex.getMessage());
                int intentos = ev.getIntentos() != null ? ev.getIntentos() + 1 : 1;
                ev.setIntentos(intentos);
                ev.setUltimoError(ex.getMessage());
                ev.setEstado("ERROR_TIPO_DESCONOCIDO");
                ev.setFechaActualizacion(LocalDateTime.now());
            } catch (Exception ex) {
                log.error("Error al procesar outbox evento ID={}: {}", ev.getId(), ex.getMessage());
                int intentos = ev.getIntentos() != null ? ev.getIntentos() + 1 : 1;
                ev.setIntentos(intentos);
                ev.setUltimoError(ex.getMessage());
                if (intentos >= 3) {
                    ev.setEstado("FALLIDO");
                }
                ev.setFechaActualizacion(LocalDateTime.now());
            }
        }
        outboxEventoRepositorio.saveAll(eventos);
    }

    private void despacharEvento(OutboxEvento ev) {
        if ("RECETA_EMITIDA".equalsIgnoreCase(ev.getTipoEvento())) {
            if (recetaRepositorio != null && pdfServicio != null && emailServicio != null) {
                RecetaDigital receta = recetaRepositorio.findById(ev.getAggregateId()).orElse(null);
                if (receta != null) {
                    byte[] pdfBytes = pdfServicio.generarRecetaPdf(receta, ev.getPayload());
                    emailServicio.enviarRecetaDigital(receta, pdfBytes);
                }
            }
        } else if ("TURNO_CREADO".equalsIgnoreCase(ev.getTipoEvento())) {
            if (turnoRepositorio != null && emailServicio != null) {
                Turno turno = turnoRepositorio.findById(ev.getAggregateId()).orElse(null);
                if (turno != null) {
                    emailServicio.enviarConfirmacionTurno(turno);
                }
            }
        } else {
            throw new IllegalArgumentException("Tipo de evento no soportado por outbox: " + ev.getTipoEvento());
        }
    }
}
