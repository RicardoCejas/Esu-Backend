package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.ProfesionalDtoRequest;
import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse; // Importación corregida
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.ProfesionalMapper;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
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

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Override
    public ProfesionalDtoResponse crearProfesional(ProfesionalDtoRequest profesionalDto) { // Tipo de retorno corregido
        Profesional profesional = profesionalMapper.paraEntidad(profesionalDto);
        String email = profesional.getUsuario().getEmail();
        if (usuarioRepositorio.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + email);
        }
        Profesional profesionalGuardado = profesionalRepositorio.save(profesional);
        return profesionalMapper.paraDto(profesionalGuardado);
    }

    @Override
    public ProfesionalDtoResponse obtenerProfesionalPorId(Long id) { // Tipo de retorno corregido
        Profesional profesional = obtenerEntidadProfesionalPorId(id);
        return profesionalMapper.paraDto(profesional);
    }

    @Override
    public List<ProfesionalDtoResponse> listarTodos() { // Tipo de retorno corregido
        return profesionalRepositorio.findAll().stream()
                .map(profesionalMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminarProfesional(Long id) {
        if (profesionalRepositorio.existsById(id)) {
            profesionalRepositorio.deleteById(id);
        } else {
            throw new NoEncontradoExcepcion("Profesional no encontrado con ID: " + id);
        }
    }

    @Override
    public List<ProfesionalDtoResponse> filtrarPorEspecialidad(Long especialidadId) { // Tipo de retorno corregido
        return profesionalRepositorio.findByEspecialidades_Id(especialidadId).stream() // Corregido: Usar findByEspecialidades_Id
                .map(profesionalMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProfesionalDtoResponse> filtrarPorCentro(Long centroSaludId) {
        return profesionalRepositorio.findByCentrosSalud_Id(centroSaludId).stream()
                .map(profesionalMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public Profesional obtenerEntidadProfesionalPorId(Long id) {
        return profesionalRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + id));
    }
}