package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.ConsultaBorradorDtoRequest;
import com.SaludUnificada.Esu.dto.response.ConsultaBorradorDtoResponse;
import com.SaludUnificada.Esu.entidad.ConsultaBorrador;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.ConsultaBorradorRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConsultaBorradorServicio {

    private final ConsultaBorradorRepositorio borradorRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final ObjectMapper objectMapper;

    public ConsultaBorradorServicio(ConsultaBorradorRepositorio borradorRepositorio,
                                    PacienteRepositorio pacienteRepositorio,
                                    ProfesionalRepositorio profesionalRepositorio,
                                    ObjectMapper objectMapper) {
        this.borradorRepositorio = borradorRepositorio;
        this.pacienteRepositorio = pacienteRepositorio;
        this.profesionalRepositorio = profesionalRepositorio;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConsultaBorradorDtoResponse guardar(ConsultaBorradorDtoRequest request) {
        Paciente paciente = pacienteRepositorio.findById(request.getPacienteId())
                .orElseThrow(() -> new NoEncontradoExcepcion(
                        "Paciente no encontrado con ID: " + request.getPacienteId()));

        Profesional profesional = profesionalRepositorio.findById(request.getProfesionalId())
                .orElseThrow(() -> new NoEncontradoExcepcion(
                        "Profesional no encontrado con ID: " + request.getProfesionalId()));

        if (Boolean.FALSE.equals(profesional.getEstadoActivo())) {
            throw new IllegalStateException(
                    "El profesional se encuentra inactivo y no puede guardar borradores clinicos.");
        }

        ConsultaBorrador borrador = borradorRepositorio
                .findTopByPacienteIdAndProfesionalIdOrderByFechaActualizacionDesc(
                        paciente.getId(), profesional.getId())
                .orElseGet(ConsultaBorrador::new);

        borrador.setPaciente(paciente);
        borrador.setProfesional(profesional);
        borrador.setTurnoId(request.getTurnoId());
        borrador.setContenido(serializar(request.getContenido()));

        return paraDto(borradorRepositorio.save(borrador));
    }

    @Transactional(readOnly = true)
    public ConsultaBorradorDtoResponse obtener(Long pacienteId, Long profesionalId) {
        ConsultaBorrador borrador = borradorRepositorio
                .findTopByPacienteIdAndProfesionalIdOrderByFechaActualizacionDesc(pacienteId, profesionalId)
                .orElseThrow(() -> new NoEncontradoExcepcion(
                        "No existe un borrador de consulta para el paciente indicado."));
        return paraDto(borrador);
    }

    private ConsultaBorradorDtoResponse paraDto(ConsultaBorrador borrador) {
        ConsultaBorradorDtoResponse dto = new ConsultaBorradorDtoResponse();
        dto.setId(borrador.getId());
        dto.setPacienteId(borrador.getPaciente() != null ? borrador.getPaciente().getId() : null);
        dto.setProfesionalId(borrador.getProfesional() != null ? borrador.getProfesional().getId() : null);
        dto.setTurnoId(borrador.getTurnoId());
        dto.setContenido(deserializar(borrador.getContenido()));
        dto.setFechaCreacion(borrador.getFechaCreacion());
        dto.setFechaActualizacion(borrador.getFechaActualizacion());
        return dto;
    }

    private String serializar(Map<String, Object> contenido) {
        try {
            return objectMapper.writeValueAsString(contenido);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("El borrador contiene datos que no se pueden guardar.", ex);
        }
    }

    private Map<String, Object> deserializar(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(contenido, new TypeReference<Map<String, Object>>() { });
        } catch (JsonProcessingException ex) {
            Map<String, Object> crudo = new LinkedHashMap<>();
            crudo.put("contenidoCrudo", contenido);
            return crudo;
        }
    }
}
