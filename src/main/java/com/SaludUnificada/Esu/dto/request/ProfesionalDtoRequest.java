package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfesionalDtoRequest {
    @NotBlank(message = "El nombre del profesional es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido del profesional es obligatorio")
    private String apellido;

    @NotBlank(message = "La matrícula del profesional es obligatoria")
    private String matricula;

    private String telefono;

    @NotNull(message = "El ID de la especialidad es obligatorio")
    private Long especialidadId;

    private Set<Long> centrosSaludIds;

    private String email;

    private String password;

    public ProfesionalDtoRequest(String nombre, String apellido, String matricula, String telefono, Long especialidadId) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.matricula = matricula;
        this.telefono = telefono;
        this.especialidadId = especialidadId;
    }
}
