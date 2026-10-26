package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.BloqueoAgendaDtoRequest;
import com.SaludUnificada.Esu.dto.response.BloqueoAgendaDtoResponse;
import com.SaludUnificada.Esu.dto.response.TurnoAfectadoDto;
import com.SaludUnificada.Esu.entidad.BloqueoAgenda;
import com.SaludUnificada.Esu.entidad.Profesional;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class BloqueoAgendaMapper {

    public BloqueoAgenda paraEntidad(BloqueoAgendaDtoRequest dto, Profesional profesional) {
        if (dto == null) {
            return null;
        }
        return BloqueoAgenda.builder()
                .profesional(profesional)
                .fechaInicio(dto.getFechaInicio())
                .fechaFin(dto.getFechaFin())
                .motivo(dto.getMotivo())
                .build();
    }

    public BloqueoAgendaDtoResponse paraDto(BloqueoAgenda entidad) {
        return paraDto(entidad, Collections.emptyList());
    }

    public BloqueoAgendaDtoResponse paraDto(BloqueoAgenda entidad, List<TurnoAfectadoDto> turnosCancelados) {
        if (entidad == null) {
            return null;
        }
        String nombre = entidad.getProfesional() != null
                ? entidad.getProfesional().getNombre() + " " + entidad.getProfesional().getApellido()
                : null;
        Long profesionalId = entidad.getProfesional() != null ? entidad.getProfesional().getId() : null;

        return BloqueoAgendaDtoResponse.builder()
                .id(entidad.getId())
                .profesionalId(profesionalId)
                .nombreProfesional(nombre)
                .fechaInicio(entidad.getFechaInicio())
                .fechaFin(entidad.getFechaFin())
                .motivo(entidad.getMotivo())
                .fechaCreacion(entidad.getFechaCreacion())
                .turnosAfectados(turnosCancelados != null ? turnosCancelados.size() : 0)
                .turnosCancelados(turnosCancelados != null ? turnosCancelados : Collections.emptyList())
                .build();
    }
}
