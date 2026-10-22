package com.SaludUnificada.Esu.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CentroSaludDtoRequest {
    private String nombre;
    private String tipo;
    private String direccion;
    private String telefono;
    private String ciudad;
    private String horarioAtencion;
    private Boolean disponibleGuardia;
}
