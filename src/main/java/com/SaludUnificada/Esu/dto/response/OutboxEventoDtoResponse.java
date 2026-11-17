package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta administrativa para eventos del patrón Outbox.
 * Omite deliberadamente los payloads clínicos o datos sensibles de pacientes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventoDtoResponse {
    private Long id;
    private String tipoEvento;
    private Long aggregateId;
    private String estado;
    private Integer intentos;
    private String ultimoError;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private LocalDateTime proximoIntento;
    private Boolean despachadoExterno;
}
