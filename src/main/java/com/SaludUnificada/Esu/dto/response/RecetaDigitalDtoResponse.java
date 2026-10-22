package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecetaDigitalDtoResponse {
    private Long id;
    private PacienteDtoResponse paciente;
    private ProfesionalDtoResponse profesional;
    private Long evolucionId;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String diagnostico;
    private String codigoCie10;
    private String indicacionesGenerales;
    private String hashIntegridad;
    private String codigoQrUrl;
    private LocalDateTime fechaCreacion;
    private List<MedicamentoRecetaDtoResponse> medicamentos;
}
