package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.ProfesionalDtoRequest;
import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.servicio.IEspecialidadServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
        profesional.setEstadoActivo(true);

        if (dto.getEspecialidadId() != null) {
            Especialidad especialidad = especialidadServicio.obtenerEntidadEspecialidadPorId(dto.getEspecialidadId());
            profesional.getEspecialidades().add(especialidad);
        }

        if (dto.getCentrosSaludIds() != null && !dto.getCentrosSaludIds().isEmpty()) {
            profesional.setCentrosSaludIds(new HashSet<>(dto.getCentrosSaludIds()));
        } else {
            profesional.setCentrosSaludIds(new HashSet<>(Set.of(1L)));
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
        dto.setEstadoActivo(profesional.getEstadoActivo() == null || profesional.getEstadoActivo());
        dto.setProximoTurnoLibre(proximoTurnoLibre);
        if (profesional.getCentrosSaludIds() != null && !profesional.getCentrosSaludIds().isEmpty()) {
            dto.setCentrosSaludIds(new HashSet<>(profesional.getCentrosSaludIds()));
        } else {
            dto.setCentrosSaludIds(new HashSet<>(Set.of(1L)));
        }
        if (profesional.getEspecialidades() != null && !profesional.getEspecialidades().isEmpty()) {
            Especialidad especialidad = profesional.getEspecialidades().iterator().next();
            dto.setEspecialidad(especialidadMapper.paraDto(especialidad));
        }

        return dto;
    }
}
