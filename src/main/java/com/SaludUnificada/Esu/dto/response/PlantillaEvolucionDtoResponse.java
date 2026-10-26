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
public class PlantillaEvolucionDtoResponse {
    private Long id;
    private String titulo;
    private Long especialidadId;
    private String especialidadNombre;
    private String estructuraAnamnesis;
    private String estructuraExamenFisico;
    private String tratamientoSugerido;
    private Boolean esPublica;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
