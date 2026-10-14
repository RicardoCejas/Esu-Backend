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
    private String email;
    private String rol;
}
