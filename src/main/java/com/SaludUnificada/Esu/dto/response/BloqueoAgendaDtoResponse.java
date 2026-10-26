package com.SaludUnificada.Esu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle del bloqueo de agenda registrado y turnos cancelados preventivamente")
public class BloqueoAgendaDtoResponse {

    @Schema(description = "ID del bloqueo registrado", example = "1")
    private Long id;

    @Schema(description = "ID del profesional afectado", example = "2")
    private Long profesionalId;

    @Schema(description = "Nombre completo del profesional", example = "Lucía González")
    private String nombreProfesional;

    @Schema(description = "Fecha y hora de inicio del bloqueo", example = "2026-11-01T08:00:00")
    private LocalDateTime fechaInicio;

    @Schema(description = "Fecha y hora de fin del bloqueo", example = "2026-11-01T14:00:00")
    private LocalDateTime fechaFin;

    @Schema(description = "Motivo de la indisponibilidad", example = "Licencia Médica por Capacitación")
    private String motivo;

    @Schema(description = "Fecha de registro de la solicitud", example = "2026-10-26T09:00:00")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Cantidad total de turnos afectados en el rango", example = "6")
    private Integer turnosAfectados;

    @Schema(description = "Lista detallada de pacientes afectados que requieren reprogramación")
    @Builder.Default
    private List<TurnoAfectadoDto> turnosCancelados = new ArrayList<>();
}
