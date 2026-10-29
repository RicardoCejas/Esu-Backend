package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.OutboxEvento;
import com.SaludUnificada.Esu.repositorio.OutboxEventoRepositorio;
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

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void procesarEventosPendientes() {
        List<OutboxEvento> eventos = outboxEventoRepositorio.findByEstadoOrderByFechaCreacionAsc("PENDIENTE", PageRequest.of(0, 50));
        if (eventos.isEmpty()) {
            return;
        }

        log.debug("Procesando {} eventos de outbox pendientes", eventos.size());
        for (OutboxEvento ev : eventos) {
            try {
                ev.setEstado("ENVIADO");
                ev.setFechaActualizacion(LocalDateTime.now());
            } catch (Exception ex) {
                log.error("Error al procesar outbox evento ID={}: {}", ev.getId(), ex.getMessage());
                ev.setIntentos(ev.getIntentos() != null ? ev.getIntentos() + 1 : 1);
                ev.setUltimoError(ex.getMessage());
                if (ev.getIntentos() >= 3) {
                    ev.setEstado("FALLIDO");
                }
                ev.setFechaActualizacion(LocalDateTime.now());
            }
        }
        outboxEventoRepositorio.saveAll(eventos);
    }
}
