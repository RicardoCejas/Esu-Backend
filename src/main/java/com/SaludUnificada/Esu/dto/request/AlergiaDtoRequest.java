package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlergiaDtoRequest {

    @NotBlank(message = "La sustancia o alérgeno es obligatorio")
    private String sustanciaAlergeno;

    @NotBlank(message = "La severidad es obligatoria (LEVE, MODERADA, GRAVE)")
    @Pattern(regexp = "(?i)^(LEVE|MODERADA|GRAVE)$", message = "La severidad debe ser LEVE, MODERADA o GRAVE")
    private String severidad;

    private LocalDate fechaDiagnostico;

    private String observaciones;
}
