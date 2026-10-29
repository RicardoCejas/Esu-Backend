package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionRecetaPublicaDtoResponse {
    private boolean valido;
    private Long id;
    private String tipoDocumento;
    private String estado;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private LocalDateTime fechaDispensacion;
    private String farmaciaDispensadora;
    private String matriculaProfesional;
    private String hashIntegridadSha256;
    private boolean integridadValida;
    private String mensaje;
}
