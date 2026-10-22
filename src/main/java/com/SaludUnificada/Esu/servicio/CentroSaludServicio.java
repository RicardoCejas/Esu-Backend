package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.CentroSaludDtoRequest;
import com.SaludUnificada.Esu.dto.response.CentroSaludDtoResponse;
import com.SaludUnificada.Esu.entidad.CentroSalud;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.CentroSaludRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CentroSaludServicio {

    @Autowired
    private CentroSaludRepositorio centroSaludRepositorio;

    public List<CentroSaludDtoResponse> listarTodos() {
        return centroSaludRepositorio.findAll().stream()
                .map(this::paraDto)
                .collect(Collectors.toList());
    }

    public CentroSaludDtoResponse obtenerPorId(Long id) {
        return paraDto(obtenerEntidad(id));
    }

    public CentroSaludDtoResponse crear(CentroSaludDtoRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del centro de salud es obligatorio.");
        }

        CentroSalud centro = new CentroSalud();
        centro.setNombre(request.getNombre().trim());
        centro.setTipo(valorOTipoPorDefecto(request.getTipo()));
        centro.setDireccion(recortar(request.getDireccion()));
        centro.setTelefono(recortar(request.getTelefono()));
        centro.setCiudad(recortar(request.getCiudad()));
        centro.setHorarioAtencion(recortar(request.getHorarioAtencion()));
        centro.setDisponibleGuardia(Boolean.TRUE.equals(request.getDisponibleGuardia()));

        return paraDto(centroSaludRepositorio.save(centro));
    }

    public CentroSaludDtoResponse actualizar(Long id, CentroSaludDtoRequest request) {
        CentroSalud centro = obtenerEntidad(id);

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            centro.setNombre(request.getNombre().trim());
        }
        if (request.getTipo() != null && !request.getTipo().isBlank()) {
            centro.setTipo(request.getTipo().trim().toUpperCase());
        }
        if (request.getDireccion() != null) {
            centro.setDireccion(recortar(request.getDireccion()));
        }
        if (request.getTelefono() != null) {
            centro.setTelefono(recortar(request.getTelefono()));
        }
        if (request.getCiudad() != null) {
            centro.setCiudad(recortar(request.getCiudad()));
        }
        if (request.getHorarioAtencion() != null) {
            centro.setHorarioAtencion(recortar(request.getHorarioAtencion()));
        }
        if (request.getDisponibleGuardia() != null) {
            centro.setDisponibleGuardia(request.getDisponibleGuardia());
        }

        return paraDto(centroSaludRepositorio.save(centro));
    }

    public void eliminar(Long id) {
        if (!centroSaludRepositorio.existsById(id)) {
            throw new NoEncontradoExcepcion("Centro de salud no encontrado con ID: " + id);
        }
        centroSaludRepositorio.deleteById(id);
    }

    public CentroSalud obtenerEntidad(Long id) {
        return centroSaludRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Centro de salud no encontrado con ID: " + id));
    }

    private CentroSaludDtoResponse paraDto(CentroSalud centro) {
        CentroSaludDtoResponse dto = new CentroSaludDtoResponse();
        dto.setId(String.valueOf(centro.getId()));
        dto.setNombre(centro.getNombre());
        dto.setTipo(centro.getTipo());
        dto.setDireccion(centro.getDireccion());
        dto.setTelefono(centro.getTelefono());
        dto.setCiudad(centro.getCiudad());
        dto.setHorarioAtencion(centro.getHorarioAtencion());
        dto.setDisponibleGuardia(centro.getDisponibleGuardia());
        return dto;
    }

    private String valorOTipoPorDefecto(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "CONSULTORIO_PRIVADO";
        }
        return tipo.trim().toUpperCase();
    }

    private String recortar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
