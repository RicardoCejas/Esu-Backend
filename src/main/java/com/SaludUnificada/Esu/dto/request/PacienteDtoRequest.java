package com.SaludUnificada.Esu.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PacienteDtoRequest {
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
    private String email;
}
