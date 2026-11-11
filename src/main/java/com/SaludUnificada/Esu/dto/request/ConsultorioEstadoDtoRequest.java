package com.SaludUnificada.Esu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para activar o pausar un consultorio físico")
public class ConsultorioEstadoDtoRequest {

    @NotNull(message = "El estado activo del consultorio es obligatorio")
    private Boolean activo;
}
