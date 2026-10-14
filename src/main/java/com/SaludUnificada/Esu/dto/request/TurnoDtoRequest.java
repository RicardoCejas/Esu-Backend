package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TurnoDtoRequest {
    @NotNull(message = "La fecha y hora del turno son obligatorias")
    private LocalDateTime fechaHora;

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El ID del profesional es obligatorio")
    private Long profesionalId;

    private String motivoTurno;
}
