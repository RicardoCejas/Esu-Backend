package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.HistoriaClinicaDtoRequest;
import com.SaludUnificada.Esu.dto.response.HistoriaClinicaDtoResponse;
import com.SaludUnificada.Esu.entidad.HistoriaClinica;
import com.SaludUnificada.Esu.mapper.HistoriaClinicaMapper;
import com.SaludUnificada.Esu.repositorio.HistoriaClinicaRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoriaClinicaServicio implements IHistoriaClinicaServicio {

    @Autowired
    private HistoriaClinicaRepositorio historiaClinicaRepositorio;

    @Autowired
    private HistoriaClinicaMapper historiaClinicaMapper;

    @Override
    public HistoriaClinicaDtoResponse crearHistoriaClinica(HistoriaClinicaDtoRequest historiaClinicaDto) {
        if (historiaClinicaDto.getPacienteId() == null) {
            throw new IllegalArgumentException("La historia clínica debe estar asociada a un paciente.");
        }
        HistoriaClinica historiaClinica = historiaClinicaMapper.paraEntidad(historiaClinicaDto);
        HistoriaClinica historiaGuardada = historiaClinicaRepositorio.save(historiaClinica);
        return historiaClinicaMapper.paraDto(historiaGuardada);
    }

    @Override
    public HistoriaClinicaDtoResponse obtenerHistoriaClinicaPorId(Long id) {
        HistoriaClinica historiaClinica = historiaClinicaRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Historia Clínica no encontrada con ID: " + id));
        return historiaClinicaMapper.paraDto(historiaClinica);
    }

    @Override
    public List<HistoriaClinicaDtoResponse> listarTodas() {
        return historiaClinicaRepositorio.findAll().stream()
                .map(historiaClinicaMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminarHistoriaClinica(Long id) {
        if (historiaClinicaRepositorio.existsById(id)) {
            historiaClinicaRepositorio.deleteById(id);
        } else {
            throw new RuntimeException("Historia Clínica no encontrada con ID: " + id);
        }
    }

    @Override
    public HistoriaClinicaDtoResponse buscarPorPacienteId(Long pacienteId) {
        HistoriaClinica historiaClinica = historiaClinicaRepositorio.findByPacienteId(pacienteId)
                .orElseThrow(() -> new RuntimeException("No se encontró historia clínica para el paciente con ID: " + pacienteId));
        return historiaClinicaMapper.paraDto(historiaClinica);
    }

    @Override
    public List<HistoriaClinicaDtoResponse> buscarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        return historiaClinicaRepositorio.findByFechaAtencionBetween(inicio, fin).stream()
                .map(historiaClinicaMapper::paraDto)
                .collect(Collectors.toList());
    }
}
