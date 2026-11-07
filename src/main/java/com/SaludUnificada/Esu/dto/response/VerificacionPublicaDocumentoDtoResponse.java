package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionPublicaDocumentoDtoResponse {
    private boolean valido;
    private boolean integridadValida;
    private String tipoDocumento; // RECETA_DIGITAL, EVOLUCION_MEDICA, EVOLUCION_ADENDA
    private String estado;
    private String fechaEmision;
    private String fechaFirma;
    private String fechaVencimiento;
    private String matriculaProfesional;
    private String farmaciaDispensadora;
    private String hashIntegridadSha256;
    private String mensaje;
}
