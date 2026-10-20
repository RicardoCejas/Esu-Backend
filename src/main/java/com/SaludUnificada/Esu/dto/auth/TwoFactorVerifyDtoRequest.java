package com.SaludUnificada.Esu.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorVerifyDtoRequest {
    @NotBlank(message = "El código no puede estar vacío")
    private String code;
}
