package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;

import java.time.LocalDate;
import java.util.List;

public interface ITurnoServicio {
    TurnoDtoResponse crearTurno(TurnoDtoRequest turno);
    TurnoDtoResponse obtenerTurnoPorId(Long id);
    List<TurnoDtoResponse> listarTodos();
    void eliminarTurno(Long id);
    List<TurnoDtoResponse> filtrarTurnos(Long profesionalId, LocalDate fecha);
    List<TurnoDtoResponse> obtenerTurnosPorPaciente(Long pacienteId);
}
