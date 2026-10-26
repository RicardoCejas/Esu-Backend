package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispensarRecetaDtoResponse {
    private Long recetaId;
    private String estadoReceta;
    private LocalDateTime fechaDispensacion;
    private String matriculaFarmacia;
    private String pacienteNombre;
    private String pacienteDni;
    private String profesionalNombre;
    private String matriculaProfesional;
    private String mensaje;
    private String hashIntegridad;
}
