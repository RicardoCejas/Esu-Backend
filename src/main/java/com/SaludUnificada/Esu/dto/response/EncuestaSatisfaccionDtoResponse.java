package com.SaludUnificada.Esu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle de respuesta de la encuesta de satisfacción asentada")
public class EncuestaSatisfaccionDtoResponse {
    private Long id;
    private Long turnoId;
    private Integer puntuacion;
    private String tiempoEsperaPercibido;
    private String comentario;
    private LocalDateTime fechaCreacion;
}
