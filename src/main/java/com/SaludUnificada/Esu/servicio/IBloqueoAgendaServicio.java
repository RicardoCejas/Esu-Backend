package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.BloqueoAgendaDtoRequest;
import com.SaludUnificada.Esu.dto.response.BloqueoAgendaDtoResponse;

import java.util.List;

public interface IBloqueoAgendaServicio {

    BloqueoAgendaDtoResponse registrarBloqueo(Long profesionalId, BloqueoAgendaDtoRequest request);

    List<BloqueoAgendaDtoResponse> listarBloqueosPorProfesional(Long profesionalId);
    void eliminarBloqueo(Long bloqueoId);
}
