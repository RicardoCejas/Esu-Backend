package com.SaludUnificada.Esu.dto.response;

import com.SaludUnificada.Esu.dto.request.MedicamentoItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecetaValidacionDtoResponse {
    private String codigoReceta;
    private String estado;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private LocalDateTime fechaDispensacion;
    private String profesionalNombre;
    private String profesionalMatricula;
    private String profesionalEspecialidad;
    private String centroSaludEmisorNombre;
    private String pacienteNombre;
    private String pacienteDni;
    private String pacienteObraSocial;
    private String pacienteNroAfiliado;
    private List<MedicamentoItemDto> medicamentos;
}
