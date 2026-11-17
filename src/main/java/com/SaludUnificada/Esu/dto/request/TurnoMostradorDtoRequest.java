package com.SaludUnificada.Esu.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoMostradorDtoRequest {

    @NotNull(message = "El ID del profesional es obligatorio")
    private Long profesionalId;

    private Long pacienteId;

    private String pacienteDni;
    private String pacienteNombre;
    private String pacienteTelefono;
    private String obraSocial;

    private LocalDateTime fechaHora;
    private String fecha;
    private String hora;

    @NotNull(message = "El centro de salud es obligatorio")
    private Long centroSaludId;
    private String centroSaludNombre;
    private String consultorioNumero;
    private String planta;

    @JsonAlias("motivoConsulta")
    private String motivoTurno;

    private BigDecimal copagoMonto;
    private String numeroBono;
}
