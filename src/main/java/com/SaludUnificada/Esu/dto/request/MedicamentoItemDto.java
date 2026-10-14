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
public class MedicamentoItemDto {
    private String nombreComercial;

    @NotBlank(message = "La droga o principio activo es obligatorio")
    private String droga;

    private String presentacion;
    private String dosisIndicada;
    private String posologia;
    private String viaAdministracion;
    private String duracion;
    private String cantidad;
}
