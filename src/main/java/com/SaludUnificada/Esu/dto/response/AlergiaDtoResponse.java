package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlergiaDtoResponse {
    private Long id;
    private Long pacienteId;
    private String sustanciaAlergeno;
    private String severidad; // LEVE, MODERADA, GRAVE
    private LocalDate fechaDiagnostico;
    private String observaciones;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
}
