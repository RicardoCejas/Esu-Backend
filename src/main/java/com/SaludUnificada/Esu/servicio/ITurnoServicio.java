package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;

import java.time.LocalDateTime; // Importación necesaria para LocalDateTime
import java.time.LocalDate;
import java.util.List;

public interface ITurnoServicio {
    TurnoDtoResponse crearTurno(TurnoDtoRequest turno, String emailAutenticado);
    List<LocalDateTime> obtenerHorariosDisponibles(Long profesionalId, LocalDate fecha);
    TurnoDtoResponse obtenerTurnoPorId(Long id);
    List<TurnoDtoResponse> listarTodos();
    void eliminarTurno(Long id);
    List<TurnoDtoResponse> filtrarTurnos(Long profesionalId, LocalDateTime fechaHora); // Corregido: Nombre y tipo de parámetro
    List<TurnoDtoResponse> obtenerTurnosPorPaciente(Long pacienteId);
}
