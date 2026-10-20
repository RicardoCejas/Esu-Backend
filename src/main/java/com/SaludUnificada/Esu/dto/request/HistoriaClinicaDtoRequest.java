package com.SaludUnificada.Esu.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoriaClinicaDtoRequest {
    private String diagnostico;
    private String motivoConsulta;
    private String tratamiento;
    private Long pacienteId;
}
