package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarnetDigitalDtoResponse {
    private Long pacienteId;
    private String nombreCompleto;
    private String dni;
    private String nroAfiliado;
    private String obraSocial;
    private String grupoSanguineo;
    private LocalDate fechaEmision;
    private Boolean estadoActivo;
    private String tokenVerificacion;
    private String qrPayload;
}
