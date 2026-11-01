package com.SaludUnificada.Esu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de alta de un consultorio físico dentro de una sede")
public class ConsultorioFisicoDtoRequest {

    @NotNull(message = "El ID del centro de salud es obligatorio")
    private Long centroSaludId;

    @NotBlank(message = "El número o código del consultorio es obligatorio")
    private String numero;

    private String planta;

    private String especialidadAsignada;

    private Boolean activo;
}
