package com.SaludUnificada.Esu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle de una notificación interna para el usuario")
public class NotificacionDtoResponse {
    private Long id;
    private String titulo;
    private String mensaje;
    private String tipo;
    private Boolean leida;
    private LocalDateTime fechaCreacion;
    private String linkAccion;
}
