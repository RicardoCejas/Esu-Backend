package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IPacienteServicio {
    PacienteDtoResponse crearPaciente(PacienteDtoRequest paciente);
    PacienteDtoResponse obtenerPacientePorId(Long id);
    List<PacienteDtoResponse> listarTodos();
    Page<PacienteDtoResponse> buscarPacientesPaginados(Pageable pageable);
    long contarPacientes();
    void eliminarPaciente(Long id);
    PacienteDtoResponse buscarPorDni(String dni);
    List<PacienteDtoResponse> buscarPorApellidoParcial(String apellido);
    Paciente obtenerEntidadPacientePorId(Long id);
    PacienteDtoResponse actualizarDatosContacto(Long id, String telefono, String obraSocial, String nroAfiliado);
    PacienteDtoResponse obtenerPerfilAutenticado(String email);
    void fusionarPacientes(Long pacienteOrigenId, Long pacienteDestinoId, String motivo, String emailAdmin);
}
