package com.SaludUnificada.Esu.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConvenioObraSocialDtoResponse {
    private Long id;
    private String nombreObraSocial;
    private String sigla;
    private String codigoRnosc;
    private String patronRegexAfiliado;
    private Boolean requiereCoseguro;
    private Boolean activo;
}
