package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EspecialidadDtoResponse {
    private Long id;
    private String nombre;
    private String descripcion;

    public EspecialidadDtoResponse(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }
}
