package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CentroSaludDtoResponse {
    private String id;
    private String nombre;
    private String tipo;
    private String direccion;
    private String telefono;
    private String ciudad;
    private String horarioAtencion;
    private Boolean disponibleGuardia;
}
