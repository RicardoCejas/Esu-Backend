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
public class PlantillaEvolucionDtoRequest {

    @NotBlank(message = "El título descriptivo de la plantilla clínica es obligatorio")
    private String titulo;

    private Long especialidadId;

    @NotBlank(message = "La estructura o macro-texto de la anamnesis es obligatoria")
    private String estructuraAnamnesis;

    private String estructuraExamenFisico;

    private String tratamientoSugerido;

    private Boolean esPublica = true;
}
