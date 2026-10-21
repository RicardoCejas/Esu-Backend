package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamiliarDtoResponse {
    private Long id;
    private Long titularId;
    private String nombre;
    private String apellido;
    private String dni;
    private String tipoVinculo;
    private String obraSocial;
    private String nroAfiliado;
    private LocalDateTime fechaCreacion;
}
