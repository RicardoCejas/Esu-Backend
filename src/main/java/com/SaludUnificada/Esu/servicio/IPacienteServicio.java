package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;

import java.util.List;

public interface IPacienteServicio {
    PacienteDtoResponse crearPaciente(PacienteDtoRequest paciente);
    PacienteDtoResponse obtenerPacientePorId(Long id);
    List<PacienteDtoResponse> listarTodos();
    void eliminarPaciente(Long id);
    PacienteDtoResponse buscarPorDni(String dni);
    List<PacienteDtoResponse> buscarPorApellidoParcial(String apellido);
    Paciente obtenerEntidadPacientePorId(Long id);
}
