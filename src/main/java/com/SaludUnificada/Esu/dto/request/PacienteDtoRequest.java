package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PacienteDtoRequest {
    @NotBlank(message = "El nombre del paciente es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido del paciente es obligatorio")
    private String apellido;

    @NotBlank(message = "El DNI del paciente es obligatorio")
    private String dni;

    private String telefono;

    @Email(message = "El formato del email no es valido")
    private String email;

    private String password;

    private LocalDate fechaNacimiento;
    private String obraSocial;
    private String grupoSanguineo;
    private String alergias;
    private String antecedentes;
}
