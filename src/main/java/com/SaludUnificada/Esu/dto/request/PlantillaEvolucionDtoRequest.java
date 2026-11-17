package com.SaludUnificada.Esu.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    private String especialidad;

    private String motivo;

    @JsonAlias("anamnesis")
    @NotBlank(message = "La estructura o macro-texto de la anamnesis es obligatoria")
    private String estructuraAnamnesis;

    @JsonAlias("examenFisico")
    private String estructuraExamenFisico;

    private String tratamientoSugerido;

    private Boolean esPublica = true;

    public PlantillaEvolucionDtoRequest(String titulo, Long especialidadId, String estructuraAnamnesis, String estructuraExamenFisico, String tratamientoSugerido, Boolean esPublica) {
        this.titulo = titulo;
        this.especialidadId = especialidadId;
        this.estructuraAnamnesis = estructuraAnamnesis;
        this.estructuraExamenFisico = estructuraExamenFisico;
        this.tratamientoSugerido = tratamientoSugerido;
        this.esPublica = esPublica;
    }
}
