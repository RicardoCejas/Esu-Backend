package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.DisponibilidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.DisponibilidadDtoResponse;
import com.SaludUnificada.Esu.entidad.DisponibilidadProfesional;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.repositorio.ConsultorioFisicoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DisponibilidadMapper {

    @Autowired(required = false)
    private ConsultorioFisicoRepositorio consultorioFisicoRepositorio;

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
                .consultorioId(dto.getConsultorioId())
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

        String consultorioNumero = null;
        String planta = null;
        if (entidad.getConsultorioId() != null && consultorioFisicoRepositorio != null) {
            var cOpt = consultorioFisicoRepositorio.findById(entidad.getConsultorioId());
            if (cOpt.isPresent()) {
                consultorioNumero = cOpt.get().getNumero();
                planta = cOpt.get().getPlanta();
            }
        }

        return DisponibilidadDtoResponse.builder()
                .id(entidad.getId())
                .profesionalId(profesionalId)
                .nombreProfesional(nombre)
                .diaSemana(entidad.getDiaSemana())
                .horaInicio(entidad.getHoraInicio())
                .horaFin(entidad.getHoraFin())
                .intervaloMinutos(entidad.getIntervaloMinutos())
                .centroSaludId(entidad.getCentroSaludId())
                .consultorioId(entidad.getConsultorioId())
                .consultorioNumero(consultorioNumero)
                .planta(planta)
                .activo(entidad.getActivo())
                .slotsGenerados(slotsGenerados)
                .build();
    }
}
