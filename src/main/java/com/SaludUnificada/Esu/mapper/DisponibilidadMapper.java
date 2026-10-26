package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.DisponibilidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.DisponibilidadDtoResponse;
import com.SaludUnificada.Esu.entidad.DisponibilidadProfesional;
import com.SaludUnificada.Esu.entidad.Profesional;
import org.springframework.stereotype.Component;

@Component
public class DisponibilidadMapper {

    public DisponibilidadProfesional paraEntidad(DisponibilidadDtoRequest dto, Profesional profesional) {
        if (dto == null) {
            return null;
        }
        return DisponibilidadProfesional.builder()
                .profesional(profesional)
                .diaSemana(dto.getDiaSemana())
                .horaInicio(dto.getHoraInicio())
                .horaFin(dto.getHoraFin())
                .intervaloMinutos(dto.getIntervaloMinutos())
                .centroSaludId(dto.getCentroSaludId())
                .activo(true)
                .build();
    }

    public DisponibilidadDtoResponse paraDto(DisponibilidadProfesional entidad) {
        return paraDto(entidad, 0);
    }

    public DisponibilidadDtoResponse paraDto(DisponibilidadProfesional entidad, int slotsGenerados) {
        if (entidad == null) {
            return null;
        }
        String nombre = entidad.getProfesional() != null
                ? entidad.getProfesional().getNombre() + " " + entidad.getProfesional().getApellido()
                : null;
        Long profesionalId = entidad.getProfesional() != null ? entidad.getProfesional().getId() : null;

        return DisponibilidadDtoResponse.builder()
                .id(entidad.getId())
                .profesionalId(profesionalId)
                .nombreProfesional(nombre)
                .diaSemana(entidad.getDiaSemana())
                .horaInicio(entidad.getHoraInicio())
                .horaFin(entidad.getHoraFin())
                .intervaloMinutos(entidad.getIntervaloMinutos())
                .centroSaludId(entidad.getCentroSaludId())
                .activo(entidad.getActivo())
                .slotsGenerados(slotsGenerados)
                .build();
    }
}
