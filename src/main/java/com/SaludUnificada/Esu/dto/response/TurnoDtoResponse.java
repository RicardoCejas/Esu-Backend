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
public class TurnoDtoResponse {
    private Long id;
    private LocalDateTime fechaHora;
    private String estado;
    private String motivoTurno;
    private PacienteResumenDto paciente;
    private ProfesionalResumenDto profesional;
}
