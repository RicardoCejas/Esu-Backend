package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoPublicoDtoRequest {

    @NotNull(message = "La fecha y hora del turno son obligatorias")
    private LocalDateTime fechaHora;

    @NotNull(message = "El ID del profesional es obligatorio")
    private Long profesionalId;

    private Long centroSaludId;
    private String centroSaludNombre;
    private String consultorioNumero;

    @Size(max = 255, message = "El motivo del turno no puede superar los 255 caracteres")
    private String motivoTurno;

    @Size(max = 64, message = "La clave de idempotencia no puede superar los 64 caracteres")
    private String idempotencyKey;

    // Datos probatorios de identidad del paciente
    @NotBlank(message = "El DNI del paciente es obligatorio")
    @Pattern(regexp = "^[0-9]{6,10}$", message = "El DNI debe contener entre 6 y 10 dígitos numéricos")
    private String dni;

    @NotBlank(message = "El nombre del paciente es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido del paciente es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    private String apellido;

    @NotBlank(message = "El teléfono de contacto es obligatorio")
    @Size(min = 6, max = 30, message = "El teléfono debe tener entre 6 y 30 caracteres")
    private String telefono;

    @Size(max = 120, message = "El correo electrónico no puede superar los 120 caracteres")
    private String email;

    @Size(max = 100, message = "La cobertura médica no puede superar los 100 caracteres")
    private String obraSocial;

    @Size(max = 50, message = "El número de afiliado no puede superar los 50 caracteres")
    private String nroAfiliado;
}
