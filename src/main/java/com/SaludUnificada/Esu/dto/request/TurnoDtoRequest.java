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

    private Long profesionalId;

    @Size(max = 255, message = "El motivo del turno no puede superar los 255 caracteres")
    private String motivoTurno;

    private Long pacienteId;
    private Long centroSaludId;
    private String centroSaludNombre;
    private String consultorioNumero;

    private String tipoAtencion; // "CONSULTA" o "ESTUDIO"
    private String servicio;
    private Long especialidadId;

    @Size(max = 64, message = "La clave de idempotencia no puede superar los 64 caracteres")
    private String idempotencyKey;

    public TurnoDtoRequest(LocalDateTime fechaHora, Long profesionalId, String motivoTurno) {
        this.fechaHora = fechaHora;
        this.profesionalId = profesionalId;
        this.motivoTurno = motivoTurno;
    }
}
