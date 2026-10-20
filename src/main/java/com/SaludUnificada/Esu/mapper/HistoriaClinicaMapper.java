package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.HistoriaClinicaDtoRequest;
import com.SaludUnificada.Esu.dto.response.HistoriaClinicaDtoResponse;
import com.SaludUnificada.Esu.entidad.HistoriaClinica;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.servicio.IPacienteServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HistoriaClinicaMapper {

    @Autowired
    private IPacienteServicio pacienteServicio;

    @Autowired
    private PacienteMapper pacienteMapper;

    public HistoriaClinica paraEntidad(HistoriaClinicaDtoRequest dto) {
        HistoriaClinica historia = new HistoriaClinica();
        historia.setDiagnostico(dto.getDiagnostico());
        historia.setMotivoConsulta(dto.getMotivoConsulta());
        historia.setTratamiento(dto.getTratamiento());

        if (dto.getPacienteId() != null) {
            Paciente paciente = pacienteServicio.obtenerEntidadPacientePorId(dto.getPacienteId());
            historia.setPaciente(paciente);
        }

        return historia;
    }

    public HistoriaClinicaDtoResponse paraDto(HistoriaClinica historia) {
        HistoriaClinicaDtoResponse dto = new HistoriaClinicaDtoResponse();
        dto.setId(historia.getId());
        dto.setDiagnostico(historia.getDiagnostico());
        dto.setMotivoConsulta(historia.getMotivoConsulta());
        dto.setTratamiento(historia.getTratamiento());
        dto.setFechaAtencion(historia.getFechaAtencion());

        if (historia.getPaciente() != null) {
            dto.setPaciente(pacienteMapper.paraResumenDto(historia.getPaciente()));
        }

        return dto;
    }
}
