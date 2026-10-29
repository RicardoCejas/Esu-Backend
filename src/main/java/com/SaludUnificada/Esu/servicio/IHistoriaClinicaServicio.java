package com.SaludUnificada.Esu.servicio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.SaludUnificada.Esu.dto.request.HistoriaClinicaDtoRequest;
import com.SaludUnificada.Esu.dto.response.HistoriaClinicaDtoResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface IHistoriaClinicaServicio {
    HistoriaClinicaDtoResponse crearHistoriaClinica(HistoriaClinicaDtoRequest historiaClinica);
    HistoriaClinicaDtoResponse obtenerHistoriaClinicaPorId(Long id);
    List<HistoriaClinicaDtoResponse> listarTodas();
    Page<HistoriaClinicaDtoResponse> listarTodasPaginado(Pageable pageable);
    Page<HistoriaClinicaDtoResponse> buscarPorRangoFechasPaginado(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);
    void eliminarHistoriaClinica(Long id);
    List<HistoriaClinicaDtoResponse> buscarPorPacienteId(Long pacienteId);
    List<HistoriaClinicaDtoResponse> buscarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin);
    long contarPorPaciente(Long pacienteId);
    Page<HistoriaClinicaDtoResponse> listarPorPacientePaginado(Long pacienteId, Pageable pageable);
    default void registrarAccesoEmergencia(Long pacienteId, String emailProfesional, String motivo) {
        registrarAccesoEmergencia(pacienteId, emailProfesional, motivo, null);
    }
    void registrarAccesoEmergencia(Long pacienteId, String emailProfesional, String motivo, HttpServletRequest request);
}
