package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.ProfesionalDtoRequest;
import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.entidad.Profesional;

import java.util.List;

public interface IProfesionalServicio {
    ProfesionalDtoResponse crearProfesional(ProfesionalDtoRequest profesional);
    ProfesionalDtoResponse obtenerProfesionalPorId(Long id);
    List<ProfesionalDtoResponse> listarTodos();
    void eliminarProfesional(Long id);
    List<ProfesionalDtoResponse> filtrarPorEspecialidad(Long especialidadId);
    Profesional obtenerEntidadProfesionalPorId(Long id);
}
