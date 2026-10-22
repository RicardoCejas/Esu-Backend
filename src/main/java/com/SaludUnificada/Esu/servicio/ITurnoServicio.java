package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.CopagoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoMostradorDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDisponibleDtoResponse;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ITurnoServicio {
    TurnoDtoResponse crearTurno(TurnoDtoRequest turno);
    TurnoDtoResponse obtenerTurnoPorId(Long id);
    List<TurnoDtoResponse> listarTodos();
    void eliminarTurno(Long id);
    List<TurnoDtoResponse> filtrarTurnos(Long profesionalId, LocalDateTime fechaHora);
    List<TurnoDtoResponse> obtenerTurnosPorPaciente(Long pacienteId);

    List<TurnoDtoResponse> obtenerTurnosPorProfesional(Long profesionalId, LocalDate fecha);
    List<TurnoDisponibleDtoResponse> obtenerDisponibles(Long profesionalId, LocalDate fecha);

    TurnoDtoResponse actualizarEstado(Long id, String estado);
    TurnoDtoResponse registrarCheckIn(Long id, String horaCheckIn);
    TurnoDtoResponse registrarCopago(Long id, CopagoDtoRequest request);
    TurnoDtoResponse crearTurnoMostrador(TurnoMostradorDtoRequest request);
    TurnoDtoResponse cancelarConJustificativo(Long id, String justificativo);
    TurnoDtoResponse llamarPaciente(Long id, String consultorio, String horaLlamado);
    void cancelarTurno(Long id);
}
