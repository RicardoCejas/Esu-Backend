package com.SaludUnificada.Esu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle de configuración de disponibilidad horaria de un profesional")
public class DisponibilidadDtoResponse {

    @Schema(description = "ID de la disponibilidad", example = "1")
    private Long id;

    @Schema(description = "ID del profesional", example = "1")
    private Long profesionalId;

    @Schema(description = "Nombre completo del profesional", example = "Carlos Martínez")
    private String nombreProfesional;

    @Schema(description = "Día de la semana configurado", example = "MONDAY")
    private DayOfWeek diaSemana;

    @Schema(description = "Hora de inicio", example = "08:00")
    private LocalTime horaInicio;

    @Schema(description = "Hora de fin", example = "14:00")
    private LocalTime horaFin;

    @Schema(description = "Intervalo de cada consulta en minutos", example = "20")
    private Integer intervaloMinutos;

    @Schema(description = "ID del centro de salud", example = "1")
    private Long centroSaludId;

    @Schema(description = "Indica si la grilla se encuentra activa", example = "true")
    private Boolean activo;

    @Schema(description = "Cantidad de nuevos cupos de turnos generados para los próximos 30 días", example = "45")
    private Integer slotsGenerados;
}
