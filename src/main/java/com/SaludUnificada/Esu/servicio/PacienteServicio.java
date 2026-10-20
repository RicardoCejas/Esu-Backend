package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.mapper.PacienteMapper;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PacienteServicio implements IPacienteServicio {

    @Autowired
    private PacienteRepositorio pacienteRepositorio;

    @Autowired
    private PacienteMapper pacienteMapper;

    @Override
    public PacienteDtoResponse crearPaciente(PacienteDtoRequest pacienteDto) {
        if (pacienteDto.getNombre() == null || pacienteDto.getNombre().isEmpty()) {
            throw new IllegalArgumentException("El nombre del paciente es obligatorio");
        }
        if (pacienteDto.getApellido() == null || pacienteDto.getApellido().isEmpty()) {
            throw new IllegalArgumentException("El apellido del paciente es obligatorio");
        }
        if (pacienteDto.getDni() == null || pacienteDto.getDni().isEmpty()) {
            throw new IllegalArgumentException("El DNI del paciente es obligatorio");
        }
        Paciente paciente = pacienteMapper.paraEntidad(pacienteDto);
        Paciente pacienteGuardado = pacienteRepositorio.save(paciente);
        return pacienteMapper.paraDto(pacienteGuardado);
    }

    @Override
    public PacienteDtoResponse obtenerPacientePorId(Long id) {
        Paciente paciente = obtenerEntidadPacientePorId(id);
        return pacienteMapper.paraDto(paciente);
    }

    @Override
    public List<PacienteDtoResponse> listarTodos() {
        return pacienteRepositorio.findAll().stream()
                .map(pacienteMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminarPaciente(Long id) {
        if (pacienteRepositorio.existsById(id)) {
            pacienteRepositorio.deleteById(id);
        } else {
            throw new RuntimeException("Paciente no encontrado con ID: " + id);
        }
    }

    @Override
    public PacienteDtoResponse buscarPorDni(String dni) {
        if (dni == null || dni.isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede ser nulo o vacío");
        }
        Paciente paciente = pacienteRepositorio.findByDni(dni)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado con DNI: " + dni));
        return pacienteMapper.paraDto(paciente);
    }

    @Override
    public List<PacienteDtoResponse> buscarPorApellidoParcial(String apellido) {
        return pacienteRepositorio.findByApellidoContainingIgnoreCase(apellido).stream()
                .map(pacienteMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public Paciente obtenerEntidadPacientePorId(Long id) {
        return pacienteRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado con ID: " + id));
    }
}
