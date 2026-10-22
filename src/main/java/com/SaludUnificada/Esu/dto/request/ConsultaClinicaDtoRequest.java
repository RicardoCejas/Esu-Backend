package com.SaludUnificada.Esu.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaClinicaDtoRequest {
    private String pacienteId;
    private String profesionalId;
    private String turnoId;
    private String motivo;
    private String diagnostico;
    private String tratamiento;
    private Map<String, String> signosVitales;
}
