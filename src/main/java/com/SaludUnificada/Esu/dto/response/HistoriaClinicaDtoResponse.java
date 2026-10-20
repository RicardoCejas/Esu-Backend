package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoriaClinicaDtoResponse {
    private Long id;
    private String diagnostico;
    private String motivoConsulta;
    private String tratamiento;
    private LocalDateTime fechaAtencion;
    private PacienteResumenDto paciente;
}
