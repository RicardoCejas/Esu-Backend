package com.SaludUnificada.Esu.dto.response;

import com.SaludUnificada.Esu.entidad.Cie10;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cie10DtoResponse {
    private Long id;
    private String codigo;
    private String descripcion;
    private String capitulo;

    public static Cie10DtoResponse desdeEntidad(Cie10 cie10) {
        return new Cie10DtoResponse(
                cie10.getId(),
                cie10.getCodigo(),
                cie10.getDescripcion(),
                cie10.getCapitulo());
    }
}
