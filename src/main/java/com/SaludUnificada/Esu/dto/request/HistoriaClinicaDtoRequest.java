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
public class HistoriaClinicaDtoRequest {
    @NotBlank(message = "El diagnóstico es obligatorio")
    private String diagnostico;

    private String motivoConsulta;

    private String tratamiento;

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;
}
