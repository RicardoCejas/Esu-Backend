package com.SaludUnificada.Esu.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfesionalDtoRequest {
    private String nombre;
    private String apellido;
    private String matricula;
    private String telefono;
    private Long especialidadId;
}
