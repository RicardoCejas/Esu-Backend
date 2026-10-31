package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DispensarRecetaDtoRequest {

    @NotBlank(message = "El código o hash de la receta es obligatorio para dispensar")
    private String codigoOQr;

    @NotBlank(message = "La matrícula o identificación de la farmacia dispensadora es obligatoria")
    private String matriculaFarmacia;

    private String farmaciaNombre;

    private String observaciones;

    public DispensarRecetaDtoRequest(String codigoOQr, String matriculaFarmacia, String observaciones) {
        this.codigoOQr = codigoOQr;
        this.matriculaFarmacia = matriculaFarmacia;
        this.observaciones = observaciones;
    }

    public DispensarRecetaDtoRequest(String codigoOQr, String matriculaFarmacia, String observaciones, String farmaciaNombre) {
        this.codigoOQr = codigoOQr;
        this.matriculaFarmacia = matriculaFarmacia;
        this.observaciones = observaciones;
        this.farmaciaNombre = farmaciaNombre;
    }
}
