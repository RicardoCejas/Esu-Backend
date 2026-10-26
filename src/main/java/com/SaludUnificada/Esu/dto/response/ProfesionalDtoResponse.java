package com.SaludUnificada.Esu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle del profesional de salud con especialidad y próximo cupo disponible")
public class ProfesionalDtoResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String matricula;
    private String telefono;
    private EspecialidadDtoResponse especialidad;

    @Schema(description = "Fecha y hora del primer turno libre disponible", example = "2026-10-27T08:30:00")
    private LocalDateTime proximoTurnoLibre;
}
