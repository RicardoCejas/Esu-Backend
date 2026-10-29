package com.SaludUnificada.Esu.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvolucionAdendaDtoResponse {
    private Long id;
    private Long evolucionId;
    private Long profesionalId;
    private String profesionalNombre;
    private String justificacionClinica;
    private String contenidoAdicional;
    private LocalDateTime fechaFirma;
    private String hashIntegridad;
}
