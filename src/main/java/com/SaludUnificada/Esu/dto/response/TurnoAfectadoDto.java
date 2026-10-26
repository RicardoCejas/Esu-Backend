package com.SaludUnificada.Esu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos del turno afectado por un bloqueo de agenda para gestión de reprogramación")
public class TurnoAfectadoDto {

    @Schema(description = "ID del turno", example = "105")
    private Long turnoId;

    @Schema(description = "Fecha y hora del turno cancelado", example = "2026-10-28T09:30:00")
    private LocalDateTime fechaHora;

    @Schema(description = "ID del paciente asignado", example = "42")
    private Long pacienteId;

    @Schema(description = "Nombre completo del paciente a reprogramar", example = "Juan Pérez")
    private String nombrePaciente;

    @Schema(description = "DNI del paciente", example = "35123456")
    private String dniPaciente;

    @Schema(description = "Teléfono de contacto del paciente", example = "03549-15448899")
    private String telefonoPaciente;

    @Schema(description = "Estado previo a la cancelación", example = "CONFIRMADO")
    private String estadoAnterior;
}
