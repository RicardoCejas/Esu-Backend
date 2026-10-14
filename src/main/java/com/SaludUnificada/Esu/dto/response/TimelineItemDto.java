package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimelineItemDto {
    private Long id;
    private String fecha;
    private String tipo; // "EVOLUCION", "RECETA"
    private String profesionalNombre;
    private String profesionalMatricula;
    private String especialidad;
    private String motivo;
    private String diagnostico;
    private String codigoCie10;
    private String descripcionCie10;
    private String tratamiento;
    private Map<String, String> signosVitales;
    private Long recetaId;
    private Boolean tieneAdendas;
    private List<String> adendas;
    private String hashIntegridad;
    private String pdfUrl;
}
