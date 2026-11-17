package com.SaludUnificada.Esu.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAuditoriaSeguridadDtoResponse {
    private Long id;
    private String email;
    private String ipAddress;
    private String userAgent;
    private String evento;
    private String detalle;
    private LocalDateTime fecha;
}
