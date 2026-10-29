package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_evento", indexes = {
    @Index(name = "idx_outbox_estado", columnList = "estado, fecha_creacion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_evento", nullable = false, length = 100)
    private String tipoEvento;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(nullable = false)
    private Integer intentos = 0;

    @Lob
    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "lease_hasta")
    private LocalDateTime leaseHasta;

    @Column(name = "proximo_intento")
    private LocalDateTime proximoIntento;

    public OutboxEvento(String tipoEvento, Long aggregateId, String payload) {
        this.tipoEvento = tipoEvento;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.estado = "PENDIENTE";
        this.intentos = 0;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }
}
