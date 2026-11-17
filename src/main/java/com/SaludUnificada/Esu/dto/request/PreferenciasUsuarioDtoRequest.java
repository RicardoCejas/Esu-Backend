package com.SaludUnificada.Esu.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasUsuarioDtoRequest {
    private Boolean altoContraste;
    private String tamanoFuente;
    private Boolean avisosEmail;
}
