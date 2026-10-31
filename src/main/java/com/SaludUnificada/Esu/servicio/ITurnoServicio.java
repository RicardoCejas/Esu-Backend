package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.AtencionTurnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.CopagoDtoRequest;
import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ITurnoServicio {
    TurnoDtoResponse crearTurno(TurnoDtoRequest turno, String emailAutenticado);
    TurnoDtoResponse crearSobreturno(SobreturnoDtoRequest request);
    List<LocalDateTime> obtenerHorariosDisponibles(Long profesionalId, LocalDate fecha);
    TurnoDtoResponse obtenerTurnoPorId(Long id);
    List<TurnoDtoResponse> listarTodos();
    Page<TurnoDtoResponse> listarPaginados(Pageable pageable);
    void eliminarTurno(Long id);
    List<TurnoDtoResponse> filtrarTurnos(Long profesionalId, LocalDateTime fechaHora);
    List<TurnoDtoResponse> obtenerTurnosPorPaciente(Long pacienteId);
    List<TurnoDtoResponse> obtenerTurnosProximosPaciente(Long pacienteId);
    List<TurnoDtoResponse> obtenerHistorialTurnosPaciente(Long pacienteId);
    TurnoDtoResponse cancelarTurno(Long id, String emailAutenticado);
    TurnoDtoResponse confirmarTurno(Long id, String emailAutenticado);
    TurnoDtoResponse reprogramarTurno(Long id, LocalDateTime nuevaFechaHora, String emailAutenticado);

    TurnoDtoResponse realizarCheckIn(Long turnoId, String emailRecepcionista);
    TurnoDtoResponse registrarCopago(Long turnoId, CopagoDtoRequest copagoDto);
    TurnoDtoResponse atenderTurno(Long turnoId, AtencionTurnoDtoRequest atencionDto, String emailProfesional);

    byte[] generarCertificadoAsistencia(Long turnoId, String emailAutenticado);
    TurnoDtoResponse marcarAusente(Long turnoId, String emailPersonal);
    List<TurnoDtoResponse> obtenerTurnosHoyProfesional(Long profesionalId);

    List<TurnoDtoResponse> obtenerAgendaProfesional(Long profesionalId, Long centroSaludId, java.time.LocalDate fecha);

    List<TurnoDtoResponse> obtenerMiAgenda(String emailProfesional, Long centroSaludId, java.time.LocalDate fecha);
}
