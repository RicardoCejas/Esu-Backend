package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.EspecialidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.EspecialidadDtoResponse;
import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.mapper.EspecialidadMapper;
import com.SaludUnificada.Esu.repositorio.EspecialidadRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EspecialidadServicio implements IEspecialidadServicio {

    @Autowired
    private EspecialidadRepositorio especialidadRepositorio;

    @Autowired
    private EspecialidadMapper especialidadMapper;

    @Override
    @CacheEvict(value = "especialidades", allEntries = true)
    public EspecialidadDtoResponse crearEspecialidad(EspecialidadDtoRequest especialidadDto) {
        if (especialidadDto.getNombre() == null || especialidadDto.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la especialidad es obligatorio y no puede estar en blanco");
        }
        String nombreNormalizado = especialidadDto.getNombre().trim();
        if (especialidadRepositorio.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new ReglaNegocioException("Ya existe una especialidad registrada con el nombre: " + nombreNormalizado);
        }
        especialidadDto.setNombre(nombreNormalizado);
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
    @Transactional
    @CacheEvict(value = "especialidades", allEntries = true)
    public EspecialidadDtoResponse actualizarEspecialidad(Long id, EspecialidadDtoRequest especialidadDto) {
        if (especialidadDto.getNombre() == null || especialidadDto.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la especialidad es obligatorio");
        }
        Especialidad especialidad = obtenerEntidadEspecialidadPorId(id);
        String nuevoNombre = especialidadDto.getNombre().trim();
        if (!especialidad.getNombre().equalsIgnoreCase(nuevoNombre) && especialidadRepositorio.existsByNombreIgnoreCase(nuevoNombre)) {
            throw new ReglaNegocioException("Ya existe otra especialidad con el nombre: " + nuevoNombre);
        }
        especialidad.setNombre(nuevoNombre);
        Especialidad guardada = especialidadRepositorio.save(especialidad);
        return especialidadMapper.paraDto(guardada);
    }

    @Override
    @Cacheable("especialidades")
    public List<EspecialidadDtoResponse> listarTodas() {
        return especialidadRepositorio.findAll().stream()
                .map(especialidadMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public long contarEspecialidades() {
        return especialidadRepositorio.count();
    }

    @Override
    @CacheEvict(value = "especialidades", allEntries = true)
    public void eliminarEspecialidad(Long id) {
        if (especialidadRepositorio.existsById(id)) {
            especialidadRepositorio.deleteById(id);
        } else {
            throw new NoEncontradoExcepcion("Especialidad no encontrada con ID: " + id);
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
                .orElseThrow(() -> new NoEncontradoExcepcion("Especialidad no encontrada con ID: " + id));
    }
}
