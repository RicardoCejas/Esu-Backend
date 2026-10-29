package com.SaludUnificada.Esu.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
