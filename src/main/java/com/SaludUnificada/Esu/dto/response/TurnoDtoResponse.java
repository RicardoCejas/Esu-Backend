package com.SaludUnificada.Esu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle de respuesta de un turno medico")
public class TurnoDtoResponse {
    private Long id;
    private LocalDateTime fechaHora;
    private String estado;
    private String motivoTurno;
    private Boolean esSobreturno;
    private String motivoSobreturno;
    private PacienteDtoResponse paciente;
    private ProfesionalDtoResponse profesional;
    private Long centroSaludId;
    private String centroSaludNombre;
    private String consultorioNumero;
private LocalDateTime fechaCheckIn;
    private String recepcionistaCheckIn;

    private BigDecimal montoCopago;
    private String numeroBono;
    private String metodoPago;
    private LocalDateTime fechaCopago;

    private String observacionesAtencion;
    private LocalDateTime fechaAtencion;

    private LocalDateTime fechaCancelacion;
    private String motivoCancelacion;
}
