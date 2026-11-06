package com.SaludUnificada.Esu.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtAuthDtoResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long usuarioId;
    private Long pacienteId;
    private Long profesionalId;
    private String email;
    private String rol;
    private String nombre;
    private String apellido;
    private String dni;
    private Boolean emailVerificado;
}
