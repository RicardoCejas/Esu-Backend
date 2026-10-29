package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrevisualizarPdfDtoRequest {
    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;
}
