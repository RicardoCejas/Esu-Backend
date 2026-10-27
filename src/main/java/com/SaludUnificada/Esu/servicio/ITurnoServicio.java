package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.AtencionTurnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.CopagoDtoRequest;
import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ITurnoServicio {
    TurnoDtoResponse crearTurno(TurnoDtoRequest turno, String emailAutenticado);
    TurnoDtoResponse crearSobreturno(SobreturnoDtoRequest request);
    List<LocalDateTime> obtenerHorariosDisponibles(Long profesionalId, LocalDate fecha);
    TurnoDtoResponse obtenerTurnoPorId(Long id);
    List<TurnoDtoResponse> listarTodos();
    void eliminarTurno(Long id);
    List<TurnoDtoResponse> filtrarTurnos(Long profesionalId, LocalDateTime fechaHora);
    List<TurnoDtoResponse> obtenerTurnosPorPaciente(Long pacienteId);
    TurnoDtoResponse cancelarTurno(Long id, String emailAutenticado);
    TurnoDtoResponse reprogramarTurno(Long id, LocalDateTime nuevaFechaHora, String emailAutenticado);

    TurnoDtoResponse realizarCheckIn(Long turnoId, String emailRecepcionista);
    TurnoDtoResponse registrarCopago(Long turnoId, CopagoDtoRequest copagoDto);
    TurnoDtoResponse atenderTurno(Long turnoId, AtencionTurnoDtoRequest atencionDto, String emailProfesional);
}
