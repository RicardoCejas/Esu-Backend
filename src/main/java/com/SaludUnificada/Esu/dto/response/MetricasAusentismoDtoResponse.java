package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricasAusentismoDtoResponse {
    private LocalDate desde;
    private LocalDate hasta;
    private Long centroId;
    private Long totalTurnos;
    private Long turnosAtendidos;
    private Long turnosAusentes;
    private Long turnosCancelados;
    private Long turnosPendientes;
    private Double tasaAusentismo;
    private Double tasaAsistencia;
    private Map<String, Long> desglosePorEstado;
    private String mensajePeriodo;
}
