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
public class EspecialidadDtoRequest {
    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    private String nombre;
}
