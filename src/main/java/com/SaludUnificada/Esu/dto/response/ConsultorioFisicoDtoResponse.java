package com.SaludUnificada.Esu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle de un consultorio físico")
public class ConsultorioFisicoDtoResponse {
    private Long id;
    private Long centroSaludId;
    private String numero;
    private String planta;
    private String especialidadAsignada;
    private Boolean activo;
}
