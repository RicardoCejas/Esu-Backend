package com.SaludUnificada.Esu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para registrar un bloqueo preventivo de agenda por licencia, feriado o vacaciones")
public class BloqueoAgendaDtoRequest {

    @NotNull(message = "La fecha y hora de inicio del bloqueo son obligatorias")
    @FutureOrPresent(message = "La fecha de inicio no puede ser anterior al momento actual")
    @Schema(description = "Inicio del período de bloqueo (ISO DateTime)", example = "2026-11-01T08:00:00")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha y hora de fin del bloqueo son obligatorias")
    @Schema(description = "Fin del período de bloqueo (ISO DateTime)", example = "2026-11-01T14:00:00")
    private LocalDateTime fechaFin;

    @NotBlank(message = "El motivo de indisponibilidad es obligatorio")
    @Size(min = 3, max = 255, message = "El motivo debe contener entre 3 y 255 caracteres")
    @Schema(description = "Motivo del bloqueo (Licencia Médica, Congreso, Vacaciones, Feriado)", example = "Licencia Médica por Capacitación")
    private String motivo;
}
