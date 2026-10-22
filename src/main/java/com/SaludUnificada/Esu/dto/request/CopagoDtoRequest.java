package com.SaludUnificada.Esu.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CopagoDtoRequest {
    private String obraSocial;
    private String numeroAfiliado;
    private BigDecimal copagoMonto;
    private String numeroBono;
}
