package com.SaludUnificada.Esu.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginDtoRequest {

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato de email no es válido")
    private String email;

    @NotBlank(message = "La contraseña no puede estar vacía")
    private String password;
}
