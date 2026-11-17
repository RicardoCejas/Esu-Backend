package com.SaludUnificada.Esu.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginDtoRequest {

    @NotBlank(message = "El token de credencial de Google es obligatorio")
    private String idToken;
}
