package com.SaludUnificada.Esu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de evaluación y satisfacción post-atención médica")
public class EncuestaSatisfaccionDtoRequest {

    @NotNull(message = "La puntuación de satisfacción es obligatoria (1 a 5)")
    @Min(value = 1, message = "La puntuación mínima es 1 estrella")
    @Max(value = 5, message = "La puntuación máxima es 5 estrellas")
    @Schema(description = "Calificación general de la atención recibida (1 a 5)", example = "5")
    private Integer puntuacion;

    @NotBlank(message = "El tiempo de espera percibido es obligatorio (EXCELENTE, ADECUADO, EXCESIVO)")
    @Schema(description = "Percepción del tiempo transcurrido en sala de espera", example = "ADECUADO")
    private String tiempoEsperaPercibido;

    @Size(max = 1000, message = "El comentario no puede superar los 1000 caracteres")
    @Schema(description = "Comentarios u observaciones adicionales sobre la atención", example = "Excelente atención del médico y trato muy cordial en recepción.")
    private String comentario;
}
