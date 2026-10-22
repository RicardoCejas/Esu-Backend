package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionMedicaDtoResponse {
    private Long id;
    private PacienteDtoResponse paciente;
    private ProfesionalDtoResponse profesional;
    private Long turnoId;
    private Long historiaClinicaId;
    private String motivoConsulta;
    private String anamnesis;
    private String examenFisico;
    private String diagnostico;
    private String codigoCie10;
    private String descripcionCie10;
    private String tratamiento;
    private String presionArterial;
    private String frecuenciaCardiaca;
    private String temperatura;
    private String peso;
    private String talla;
    private String imc;
    private Boolean firmada;
    private LocalDateTime fechaFirma;
    private String hashIntegridad;
    private LocalDateTime fechaCreacion;
    private List<EvolucionAdendaDtoResponse> adendas;
    private List<RecetaDigitalDtoResponse> recetas;
}
