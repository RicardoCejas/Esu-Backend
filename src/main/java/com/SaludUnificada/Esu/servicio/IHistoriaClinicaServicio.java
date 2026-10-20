package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.HistoriaClinicaDtoRequest;
import com.SaludUnificada.Esu.dto.response.HistoriaClinicaDtoResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface IHistoriaClinicaServicio {
    HistoriaClinicaDtoResponse crearHistoriaClinica(HistoriaClinicaDtoRequest historiaClinica);
    HistoriaClinicaDtoResponse obtenerHistoriaClinicaPorId(Long id);
    List<HistoriaClinicaDtoResponse> listarTodas();
    void eliminarHistoriaClinica(Long id);
    HistoriaClinicaDtoResponse buscarPorPacienteId(Long pacienteId);
    List<HistoriaClinicaDtoResponse> buscarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin);
}
