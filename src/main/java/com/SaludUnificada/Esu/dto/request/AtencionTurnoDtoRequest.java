package com.SaludUnificada.Esu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de finalización y atención médica de un turno")
public class AtencionTurnoDtoRequest {

    @NotBlank(message = "Las observaciones médicas o diagnóstico de la atención son obligatorias")
    @Schema(description = "Observaciones clínicas de la consulta o diagnóstico de cierre", example = "Paciente evaluado con cuadro clínico favorable. Se indica control en 15 días.")
    private String observaciones;
}
