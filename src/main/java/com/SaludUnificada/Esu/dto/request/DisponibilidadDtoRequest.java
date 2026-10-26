package com.SaludUnificada.Esu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para registrar o actualizar la disponibilidad horaria de un profesional")
public class DisponibilidadDtoRequest {

    @NotNull(message = "El ID del profesional es obligatorio")
    @Schema(description = "ID del profesional de salud", example = "1")
    private Long profesionalId;

    @NotNull(message = "El día de la semana es obligatorio")
    @Schema(description = "Día de atención semanal", example = "MONDAY")
    private DayOfWeek diaSemana;

    @NotNull(message = "La hora de inicio es obligatoria")
    @Schema(description = "Hora de inicio de atención (HH:mm)", example = "08:00")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    @Schema(description = "Hora de finalización de atención (HH:mm)", example = "14:00")
    private LocalTime horaFin;

    @NotNull(message = "El intervalo en minutos es obligatorio")
    @Min(value = 5, message = "El intervalo mínimo es de 5 minutos")
    @Max(value = 120, message = "El intervalo máximo es de 120 minutos")
    @Schema(description = "Duración en minutos de cada consulta (15, 20 o 30 min)", example = "20")
    private Integer intervaloMinutos;

    @Schema(description = "ID del centro médico o sede donde presta servicios", example = "1")
    private Long centroSaludId;
}
