package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfesionalDtoRequest {
    @NotBlank(message = "El nombre del profesional es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido del profesional es obligatorio")
    private String apellido;

    @NotBlank(message = "La matrícula del profesional es obligatoria")
    private String matricula;

    private String telefono;

    @NotNull(message = "El ID de la especialidad es obligatorio")
    private Long especialidadId;

    @Email(message = "El email del profesional no es válido")
    private String email;

    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
}
