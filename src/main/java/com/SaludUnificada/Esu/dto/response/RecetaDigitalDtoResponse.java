package com.SaludUnificada.Esu.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaDigitalDtoResponse {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private String pacienteDni;
    private Long profesionalId;
    private String profesionalNombre;
    private String profesionalMatricula;
    private Long centroSaludEmisorId;
    private String centroSaludEmisorNombre;
    private String farmaciaDispensadoraNombre;
    private String lugarRetiroSugerido;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String diagnostico;
    private String codigoCie10;
    private String indicacionesGenerales;
    private String estadoReceta;
    private LocalDateTime fechaDispensacion;
    private String matriculaFarmaciaDispensadora;
    private String hashIntegridad;
    private String codigoQrUrl;
    private String motivoAnulacion;
    private LocalDateTime fechaAnulacion;
    private List<MedicamentoRecetaDtoResponse> medicamentos;
}
