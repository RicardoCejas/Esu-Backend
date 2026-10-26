package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamiliarDtoRequest {
    @NotBlank(message = "El nombre del familiar es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido del familiar es obligatorio")
    private String apellido;

    @NotBlank(message = "El DNI del familiar es obligatorio")
    private String dni;

    @NotBlank(message = "El tipo de vínculo es obligatorio")
    private String tipoVinculo;

    private String obraSocial;
    private String nroAfiliado;
}
