package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.DisponibilidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.DisponibilidadDtoResponse;

import java.util.List;

public interface IDisponibilidadServicio {

    DisponibilidadDtoResponse configurarDisponibilidad(DisponibilidadDtoRequest request);

    List<DisponibilidadDtoResponse> obtenerDisponibilidadPorProfesional(Long profesionalId);

    int generarSlotsProximos30Dias(Long profesionalId);
    void eliminarDisponibilidad(Long id);
}
