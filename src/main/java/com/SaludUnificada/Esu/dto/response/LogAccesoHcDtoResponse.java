package com.SaludUnificada.Esu.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAccesoHcDtoResponse {
    private Long id;
    private Long usuarioId;
    private String usuarioEmail;
    private String rolUsuario;
    private String pacienteIdentificador;
    private String accion;
    private String motivo;
    private String ipAddress;
    private LocalDateTime fechaHora;
    private String centroCustodia;
}
