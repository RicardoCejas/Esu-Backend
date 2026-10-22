package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionAdendaDtoResponse {
    private Long id;
    private Long evolucionId;
    private ProfesionalDtoResponse profesional;
    private String justificacionClinica;
    private String contenidoAdicional;
    private LocalDateTime fechaFirma;
    private String hashIntegridad;
}
