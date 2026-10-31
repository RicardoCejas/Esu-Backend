package com.SaludUnificada.Esu.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetricasDtoResponse {

    // Metadatos y contexto de consulta
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private Long centroId;
    private String centroNombre;
    private String especialidad;
    private LocalDateTime fechaActualizacion;
    private Boolean respuestaIncompleta;
    private String periodoDescripcion;

    // Métricas globales agregadas
    private long totalTurnos;
    private long turnosAtendidos;
    private long turnosAusentes;
    private long turnosCancelados;
    private long turnosPendientes;
    private long turnosHoy;
    private long atendidosHoy;
    private long ausentesHoy;

    // Tasas con denominadores explícitos
    // Tasa Asistencia: Atendidos / Concretados (Atendidos + Ausentes)
    private double tasaAsistencia;
    // Tasa Ausentismo: Ausentes / Concretados (Atendidos + Ausentes)
    private double tasaAusentismo;
    // Tasa Cancelación: Cancelados / Total
    private double tasaCancelacion;

    // Tiempos promedio
    private double tiempoPromedioConsultaMinutos;

    // Especialidad más demandada
    private String especialidadMasDemandada;
    private long totalEspecialidadMasDemandada;

    // Gráficos y series calculadas dinámicamente desde la BD
    private List<DemandaDiaDto> demandaSemanal;
    private List<EspecialidadDemandaDto> distribucionEspecialidades;
    private List<TendenciaMensualDto> evolucionMensual;
    private Map<String, Long> desglosePorEstado;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DemandaDiaDto {
        private String diaSemana;
        private String label;
        private long atendidos;
        private long ausentes;
        private long cancelados;
        private long total;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EspecialidadDemandaDto {
        private String especialidad;
        private long totalTurnos;
        private double porcentaje;
        private String color;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TendenciaMensualDto {
        private String mes;
        private String label;
        private long totalTurnos;
        private long atendidos;
        private long ausentes;
        private double tasaAsistencia;
        private double tasaAusentismo;
    }
}
