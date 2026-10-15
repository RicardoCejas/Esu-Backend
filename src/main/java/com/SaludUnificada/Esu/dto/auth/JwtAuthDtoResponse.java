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
    private String tokenType;
    private Long usuarioId;
    private String email;
    private String rol;
    private Boolean twoFactorEnabled;
    private Boolean emailVerified;
    private Boolean requires2FA;
    private String tempToken;
}
