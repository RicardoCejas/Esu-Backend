package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.EspecialidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.EspecialidadDtoResponse;
import com.SaludUnificada.Esu.entidad.Especialidad;
import org.springframework.stereotype.Component;

@Component
public class EspecialidadMapper {

    public Especialidad paraEntidad(EspecialidadDtoRequest dto) {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(dto.getNombre());
        if (dto.getDescripcion() != null && !dto.getDescripcion().isBlank()) {
            especialidad.setDescripcion(dto.getDescripcion().trim());
        }
        especialidad.setCategoria(dto.getCategoria() != null ? dto.getCategoria() : "CONSULTA");
        especialidad.setNombreCanonico(dto.getNombreCanonico() != null ? dto.getNombreCanonico() : dto.getNombre());
        return especialidad;
    }

    public EspecialidadDtoResponse paraDto(Especialidad especialidad) {
        EspecialidadDtoResponse dto = new EspecialidadDtoResponse();
        dto.setId(especialidad.getId());
        dto.setNombre(especialidad.getNombre());
        dto.setDescripcion(especialidad.getDescripcion() != null && !especialidad.getDescripcion().isBlank()
                ? especialidad.getDescripcion()
                : "Atención médica especializada en " + especialidad.getNombre() + ".");
        dto.setCategoria(especialidad.getCategoria() != null ? especialidad.getCategoria() : "CONSULTA");
        dto.setNombreCanonico(especialidad.getNombreCanonico() != null ? especialidad.getNombreCanonico() : especialidad.getNombre());
        return dto;
    }
}
