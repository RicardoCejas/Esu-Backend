package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentoRecetaDtoResponse {
    private Long id;
    private String nombreComercial;
    private String droga;
    private String presentacion;
    private String dosisIndicada;
    private String posologia;
    private String viaAdministracion;
    private String duracion;
    private String cantidad;
}
