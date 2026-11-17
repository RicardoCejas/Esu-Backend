package com.SaludUnificada.Esu.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaBorradorDtoRequest {
    private Long pacienteId;
    private Long turnoId;
    private String motivoConsulta;
    private String anamnesis;
    private String examenFisico;
    private String diagnostico;
    private String codigoCie10;
    private String tratamiento;
}
