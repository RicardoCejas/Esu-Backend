package com.SaludUnificada.Esu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de actualización de un consultorio físico")
public class ConsultorioFisicoUpdateDtoRequest {

    @NotBlank(message = "El número o código del consultorio es obligatorio")
    private String numero;

    private String planta;

    private String especialidadAsignada;

    private Boolean activo;
}
