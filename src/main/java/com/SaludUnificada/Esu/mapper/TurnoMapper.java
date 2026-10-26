package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.Turno;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TurnoMapper {

    @Autowired
    private PacienteMapper pacienteMapper;

    @Autowired
    private ProfesionalMapper profesionalMapper;

    public TurnoDtoResponse paraDto(Turno turno) {
        if (turno == null) {
            return null;
        }
        TurnoDtoResponse dto = new TurnoDtoResponse();
        dto.setId(turno.getId());
        dto.setFechaHora(turno.getFechaHora());
        dto.setEstado(turno.getEstado());
        dto.setMotivoTurno(turno.getMotivoTurno());
        dto.setEsSobreturno(Boolean.TRUE.equals(turno.getEsSobreturno()));
        dto.setMotivoSobreturno(turno.getMotivoSobreturno());

        if (turno.getPaciente() != null) {
            dto.setPaciente(pacienteMapper.paraDto(turno.getPaciente()));
        }

        if (turno.getProfesional() != null) {
            dto.setProfesional(profesionalMapper.paraDto(turno.getProfesional()));
        }

        return dto;
    }
}
