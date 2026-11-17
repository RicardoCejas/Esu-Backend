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
public class SolicitarRecuperacionDtoRequest {

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "Formato de email no válido")
    private String email;
}
