package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfesionalDtoResponse { // Nombre de la clase actualizado
    private Long id;
    private String nombre;
    private String apellido;
    private String matricula;
    private String telefono;
    private String email;
    private EspecialidadDtoResponse especialidad;
    private List<String> centrosSaludIds;
    private List<String> diasAtencion;
    private Integer duracionTurnoMin;
}
