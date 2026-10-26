package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopDiagnosticoDtoResponse {
    private String codigoCie10;
    private String descripcionCie10;
    private Long totalCasos;
    private Double porcentaje;

    public TopDiagnosticoDtoResponse(String codigoCie10, String descripcionCie10, Long totalCasos) {
        this.codigoCie10 = codigoCie10;
        this.descripcionCie10 = descripcionCie10;
        this.totalCasos = totalCasos;
        this.porcentaje = 0.0;
    }
}
