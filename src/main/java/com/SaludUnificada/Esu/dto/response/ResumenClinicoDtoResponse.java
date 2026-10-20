package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumenClinicoDtoResponse {
    private Long pacienteId;
    private String nombreCompleto;
    private String dni;
    private String fechaNacimiento;
    private Integer edad;
    private String obraSocial;
    private String nroAfiliado;
    private String grupoSanguineo;
    private String telefono;

    // Alertas Médicas Críticas (Alergias graves destacadas en rojo)
    private List<String> alertasCriticas;

    // Antecedentes Personales y Patológicos
    private List<String> antecedentes;

    // Últimos Signos Vitales registrados
    private Map<String, String> ultimosSignosVitales;

    private String ultimaConsultaFecha;
    private String ultimoDiagnostico;
}
