package com.SaludUnificada.Esu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle de respuesta de un turno médico")
public class TurnoDtoResponse {
    private Long id;
    private LocalDateTime fechaHora;
    private String estado;
    private String motivoTurno;
    private Boolean esSobreturno;
    private String motivoSobreturno;
    private PacienteDtoResponse paciente;
    private ProfesionalDtoResponse profesional;
}
