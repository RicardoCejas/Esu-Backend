package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionAdendaDtoRequest {

    @NotNull(message = "El ID del profesional es obligatorio")
    private Long profesionalId;

    @NotBlank(message = "La justificación clínica de la adenda es obligatoria")
    private String justificacionClinica;

    @NotBlank(message = "El contenido aclaratorio de la adenda es obligatorio")
    private String contenidoAdicional;
}
