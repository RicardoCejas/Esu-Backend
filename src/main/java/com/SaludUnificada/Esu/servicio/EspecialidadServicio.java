package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.EspecialidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.EspecialidadDtoResponse;
import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.mapper.EspecialidadMapper;
import com.SaludUnificada.Esu.repositorio.EspecialidadRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EspecialidadServicio implements IEspecialidadServicio {

    @Autowired
    private EspecialidadRepositorio especialidadRepositorio;

    @Autowired
    private EspecialidadMapper especialidadMapper;

    @Override
    public EspecialidadDtoResponse crearEspecialidad(EspecialidadDtoRequest especialidadDto) {
        if (especialidadDto.getNombre() == null || especialidadDto.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la especialidad es obligatorio y no puede estar en blanco");
        }
        Especialidad especialidad = especialidadMapper.paraEntidad(especialidadDto);
        Especialidad especialidadGuardada = especialidadRepositorio.save(especialidad);
        return especialidadMapper.paraDto(especialidadGuardada);
    }

    @Override
    public EspecialidadDtoResponse obtenerEspecialidadPorId(Long id) {
        Especialidad especialidad = obtenerEntidadEspecialidadPorId(id);
        return especialidadMapper.paraDto(especialidad);
    }

    @Override
    public List<EspecialidadDtoResponse> listarTodas() {
        return especialidadRepositorio.findAll().stream()
                .map(especialidadMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminarEspecialidad(Long id) {
        if (especialidadRepositorio.existsById(id)) {
            especialidadRepositorio.deleteById(id);
        } else {
            throw new RuntimeException("Especialidad no encontrada con ID: " + id);
        }
    }

    @Override
    public List<EspecialidadDtoResponse> buscarPorNombre(String nombre) {
        return especialidadRepositorio.findByNombreContainingIgnoreCase(nombre).stream()
                .map(especialidadMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public Especialidad obtenerEntidadEspecialidadPorId(Long id) {
        return especialidadRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Especialidad no encontrada con ID: " + id));
    }
}
