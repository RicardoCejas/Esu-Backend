package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.servicio.IPacienteServicio;
import com.SaludUnificada.Esu.servicio.IProfesionalServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TurnoMapper {

    @Autowired
    private IPacienteServicio pacienteServicio;

    @Autowired
    private IProfesionalServicio profesionalServicio;

    @Autowired
    private PacienteMapper pacienteMapper;

    @Autowired
    private ProfesionalMapper profesionalMapper;

    public Turno paraEntidad(TurnoDtoRequest dto) {
        Turno turno = new Turno();
        turno.setFechaHora(dto.getFechaHora());
        turno.setMotivoTurno(dto.getMotivoTurno());
        turno.setEstado("PENDIENTE"); // Estado por defecto

        if (dto.getPacienteId() != null) {
            Paciente paciente = pacienteServicio.obtenerEntidadPacientePorId(dto.getPacienteId());
            turno.setPaciente(paciente);
        }

        if (dto.getProfesionalId() != null) {
            Profesional profesional = profesionalServicio.obtenerEntidadProfesionalPorId(dto.getProfesionalId());
            turno.setProfesional(profesional);
        }

        return turno;
    }

    public TurnoDtoResponse paraDto(Turno turno) {
        TurnoDtoResponse dto = new TurnoDtoResponse();
        dto.setId(turno.getId());
        dto.setFechaHora(turno.getFechaHora());
        dto.setEstado(turno.getEstado());
        dto.setMotivoTurno(turno.getMotivoTurno());

        if (turno.getPaciente() != null) {
            dto.setPaciente(pacienteMapper.paraResumenDto(turno.getPaciente()));
        }

        if (turno.getProfesional() != null) {
            dto.setProfesional(profesionalMapper.paraResumenDto(turno.getProfesional()));
        }

        return dto;
    }
}
