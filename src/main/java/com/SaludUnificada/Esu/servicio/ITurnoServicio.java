package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface ITurnoServicio {
    TurnoDtoResponse crearTurno(TurnoDtoRequest turno);
    TurnoDtoResponse crearSobreturno(SobreturnoDtoRequest request);
    TurnoDtoResponse obtenerTurnoPorId(Long id);
    List<TurnoDtoResponse> listarTodos();
    void eliminarTurno(Long id);
    List<TurnoDtoResponse> filtrarTurnos(Long profesionalId, LocalDateTime fechaHora);
    List<TurnoDtoResponse> obtenerTurnosPorPaciente(Long pacienteId);
}
