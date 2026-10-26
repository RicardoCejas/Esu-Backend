package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstudioAdjuntoDtoResponse {
    private Long id;
    private Long pacienteId;
    private Long evolucionId;
    private String nombreArchivo;
    private String mimeType;
    private Long tamanioBytes;
    private String checksumSha256;
    private String tipoEstudio;
    private LocalDate fechaEstudio;
    private LocalDateTime fechaCarga;
    private String urlDescarga;
}
