package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.ProfesionalDtoRequest;
import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.mapper.ProfesionalMapper;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfesionalServicio implements IProfesionalServicio {

    @Autowired
    private ProfesionalRepositorio profesionalRepositorio;

    @Autowired
    private ProfesionalMapper profesionalMapper;

    @Override
    public ProfesionalDtoResponse crearProfesional(ProfesionalDtoRequest profesionalDto) {
        if (profesionalDto.getMatricula() == null || profesionalDto.getMatricula().isBlank()) {
            throw new IllegalArgumentException("La matrícula del profesional es obligatoria");
        }
        Profesional profesional = profesionalMapper.paraEntidad(profesionalDto);
        Profesional profesionalGuardado = profesionalRepositorio.save(profesional);
        return profesionalMapper.paraDto(profesionalGuardado);
    }

    @Override
    public ProfesionalDtoResponse obtenerProfesionalPorId(Long id) {
        Profesional profesional = obtenerEntidadProfesionalPorId(id);
        return profesionalMapper.paraDto(profesional);
    }

    @Override
    public List<ProfesionalDtoResponse> listarTodos() {
        return profesionalRepositorio.findAll().stream()
                .map(profesionalMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminarProfesional(Long id) {
        if (profesionalRepositorio.existsById(id)) {
            profesionalRepositorio.deleteById(id);
        } else {
            throw new RuntimeException("Profesional no encontrado con ID: " + id);
        }
    }

    @Override
    public List<ProfesionalDtoResponse> filtrarPorEspecialidad(Long especialidadId) {
        return profesionalRepositorio.findByEspecialidadId(especialidadId).stream()
                .map(profesionalMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public Profesional obtenerEntidadProfesionalPorId(Long id) {
        return profesionalRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado con ID: " + id));
    }
}
