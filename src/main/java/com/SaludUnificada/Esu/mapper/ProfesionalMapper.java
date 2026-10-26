package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.ProfesionalDtoRequest;
import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.servicio.IEspecialidadServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProfesionalMapper {

    @Autowired
    private IEspecialidadServicio especialidadServicio;

    @Autowired
    private EspecialidadMapper especialidadMapper;

    public Profesional paraEntidad(ProfesionalDtoRequest dto) {
        if (dto == null) {
            return null;
        }
        Profesional profesional = new Profesional();
        profesional.setNombre(dto.getNombre());
        profesional.setApellido(dto.getApellido());
        profesional.setMatricula(dto.getMatricula());
        profesional.setTelefono(dto.getTelefono());

        if (dto.getEspecialidadId() != null) {
            Especialidad especialidad = especialidadServicio.obtenerEntidadEspecialidadPorId(dto.getEspecialidadId());
            profesional.getEspecialidades().add(especialidad);
        }

        return profesional;
    }

    public ProfesionalDtoResponse paraDto(Profesional profesional) {
        return paraDto(profesional, null);
    }

    public ProfesionalDtoResponse paraDto(Profesional profesional, LocalDateTime proximoTurnoLibre) {
        if (profesional == null) {
            return null;
        }
        ProfesionalDtoResponse dto = new ProfesionalDtoResponse();
        dto.setId(profesional.getId());
        dto.setNombre(profesional.getNombre());
        dto.setApellido(profesional.getApellido());
        dto.setMatricula(profesional.getMatricula());
        dto.setTelefono(profesional.getTelefono());
        dto.setProximoTurnoLibre(proximoTurnoLibre);

        if (!profesional.getEspecialidades().isEmpty()) {
            Especialidad especialidad = profesional.getEspecialidades().iterator().next();
            dto.setEspecialidad(especialidadMapper.paraDto(especialidad));
        }

        return dto;
    }
}
