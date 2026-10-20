package com.SaludUnificada.Esu.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSetupDtoResponse {
    private String secret;
    private String qrCodeUrl;
    private List<String> backupCodes;
}
