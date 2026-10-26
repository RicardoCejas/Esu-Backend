package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DispensarRecetaDtoRequest {

    @NotBlank(message = "El código o hash de la receta es obligatorio para dispensar")
    private String codigoOQr;

    @NotBlank(message = "La matrícula o identificación de la farmacia dispensadora es obligatoria")
    private String matriculaFarmacia;

    private String observaciones;
}
