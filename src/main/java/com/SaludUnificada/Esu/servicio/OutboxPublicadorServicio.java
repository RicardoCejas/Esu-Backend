package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.OutboxEvento;
import com.SaludUnificada.Esu.repositorio.OutboxEventoRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublicadorServicio {

    private final OutboxEventoRepositorio outboxEventoRepositorio;

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvento registrarEvento(String tipoEvento, Long aggregateId, String payload) {
        OutboxEvento evento = new OutboxEvento();
        evento.setTipoEvento(tipoEvento);
        evento.setAggregateId(aggregateId);
        evento.setPayload(payload != null ? payload : "{}");
        evento.setEstado("PENDIENTE");
        evento.setIntentos(0);
        evento.setFechaCreacion(LocalDateTime.now());
        OutboxEvento guardado = outboxEventoRepositorio.save(evento);
        log.info("Outbox evento registrado: ID={}, Tipo={}, AggregateID={}", guardado.getId(), tipoEvento, aggregateId);
        return guardado;
    }
}
