package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.EspecialidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.EspecialidadDtoResponse;
import com.SaludUnificada.Esu.entidad.Especialidad;

import java.util.List;

public interface IEspecialidadServicio {
    EspecialidadDtoResponse crearEspecialidad(EspecialidadDtoRequest especialidad);
    EspecialidadDtoResponse obtenerEspecialidadPorId(Long id);
    List<EspecialidadDtoResponse> listarTodas();
    void eliminarEspecialidad(Long id);
    List<EspecialidadDtoResponse> buscarPorNombre(String nombre);
    Especialidad obtenerEntidadEspecialidadPorId(Long id);
}
