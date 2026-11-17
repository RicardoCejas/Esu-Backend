package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordatorioTurnoDtoResponse {
    private Long id;
    private Long turnoId;
    private LocalDateTime fechaHoraTurno;
    private String nombreProfesional;
    private LocalDateTime disponibleDesde;
    private boolean leido;
}
