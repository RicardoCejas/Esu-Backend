package com.SaludUnificada.Esu.dto.request;



import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;

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



    @NotNull(message = "El ID del profesional es obligatorio")

    private Long profesionalId;



    @Size(max = 255, message = "El motivo del turno no puede superar los 255 caracteres")

    private String motivoTurno;



    // Permite a personal administrativo y de recepción agendar turnos para un paciente específico

    private Long pacienteId;
    private Long centroSaludId;
    private String centroSaludNombre;
    private String consultorioNumero;
@Size(max = 64, message = "La clave de idempotencia no puede superar los 64 caracteres")
    private String idempotencyKey;



    public TurnoDtoRequest(LocalDateTime fechaHora, Long profesionalId, String motivoTurno) {

        this.fechaHora = fechaHora;

        this.profesionalId = profesionalId;

        this.motivoTurno = motivoTurno;

    }

}

