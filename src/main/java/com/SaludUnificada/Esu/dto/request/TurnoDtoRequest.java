package com.SaludUnificada.Esu.dto.request;

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
    private LocalDateTime fechaHora;
    private Long pacienteId;
    private Long profesionalId;
    private String motivoTurno;
}
