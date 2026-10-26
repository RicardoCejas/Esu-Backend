package com.SaludUnificada.Esu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Solicitud para registrar un sobreturno médico de urgencia en sala (HU-BE-91 / HU-FE-63)")
public class SobreturnoDtoRequest {

    @NotNull(message = "El ID del profesional es obligatorio")
    @Schema(description = "ID del médico que atenderá el sobreturno", example = "1")
    private Long profesionalId;

    @NotNull(message = "El ID del paciente es obligatorio")
    @Schema(description = "ID del paciente a ingresar como sobreturno", example = "5")
    private Long pacienteId;

    @NotNull(message = "La fecha y hora del sobreturno son obligatorias")
    @Schema(description = "Fecha y hora estimada de atención (ISO DateTime)", example = "2026-10-26T11:45:00")
    private LocalDateTime fechaHora;

    @NotBlank(message = "El motivo de sobreturno es obligatorio")
    @Size(min = 3, max = 255, message = "El motivo debe tener entre 3 y 255 caracteres")
    @Schema(description = "Motivo de la urgencia menor o demanda espontánea", example = "Demanda espontánea en sala por cuadro febril")
    private String motivoSobreturno;
}
