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
public class ConsentimientoDtoRequest {

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El ID del profesional es obligatorio")
    private Long profesionalId;

    @NotBlank(message = "El nombre del procedimiento médico es obligatorio")
    private String procedimiento;

    @NotBlank(message = "El texto legal del consentimiento es obligatorio")
    private String textoLegal;

    @NotBlank(message = "La firma digital manuscrita o PIN en base64 es obligatoria")
    private String firmaDigitalBase64;
}
