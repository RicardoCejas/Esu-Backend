package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaBorradorDtoResponse {
    private Long id;
    private Long pacienteId;
    private Long profesionalId;
    private Long turnoId;
    private Map<String, Object> contenido;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
