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
        return especialidad;
    }

    public EspecialidadDtoResponse paraDto(Especialidad especialidad) {
        EspecialidadDtoResponse dto = new EspecialidadDtoResponse();
        dto.setId(especialidad.getId());
        dto.setNombre(especialidad.getNombre());
        return dto;
    }
}
