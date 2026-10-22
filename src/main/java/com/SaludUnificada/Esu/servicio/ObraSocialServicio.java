package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.ObraSocialDtoRequest;
import com.SaludUnificada.Esu.dto.response.ObraSocialDtoResponse;
import com.SaludUnificada.Esu.entidad.ObraSocial;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.ObraSocialRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ObraSocialServicio {

    @Autowired
    private ObraSocialRepositorio obraSocialRepositorio;

    public List<ObraSocialDtoResponse> listarTodas() {
        return obraSocialRepositorio.findAll().stream()
                .map(this::paraDto)
                .collect(Collectors.toList());
    }

    public ObraSocialDtoResponse obtenerPorId(Long id) {
        return paraDto(obtenerEntidad(id));
    }

    public ObraSocialDtoResponse crear(ObraSocialDtoRequest request) {
        validar(request);
        if (obraSocialRepositorio.findByCodigo(request.getCodigo().trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una obra social con el codigo: " + request.getCodigo());
        }

        ObraSocial obraSocial = new ObraSocial();
        obraSocial.setCodigo(request.getCodigo().trim().toUpperCase());
        obraSocial.setNombre(request.getNombre().trim());
        obraSocial.setActivo(request.getActivo() == null || request.getActivo());

        return paraDto(obraSocialRepositorio.save(obraSocial));
    }

    public ObraSocialDtoResponse actualizar(Long id, ObraSocialDtoRequest request) {
        ObraSocial obraSocial = obtenerEntidad(id);

        if (request.getCodigo() != null && !request.getCodigo().isBlank()) {
            obraSocial.setCodigo(request.getCodigo().trim().toUpperCase());
        }
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            obraSocial.setNombre(request.getNombre().trim());
        }
        if (request.getActivo() != null) {
            obraSocial.setActivo(request.getActivo());
        }

        return paraDto(obraSocialRepositorio.save(obraSocial));
    }

    public void eliminar(Long id) {
        if (!obraSocialRepositorio.existsById(id)) {
            throw new NoEncontradoExcepcion("Obra social no encontrada con ID: " + id);
        }
        obraSocialRepositorio.deleteById(id);
    }

    private void validar(ObraSocialDtoRequest request) {
        if (request.getCodigo() == null || request.getCodigo().isBlank()) {
            throw new IllegalArgumentException("El codigo de la obra social es obligatorio.");
        }
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la obra social es obligatorio.");
        }
    }

    private ObraSocial obtenerEntidad(Long id) {
        return obraSocialRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Obra social no encontrada con ID: " + id));
    }

    private ObraSocialDtoResponse paraDto(ObraSocial obraSocial) {
        ObraSocialDtoResponse dto = new ObraSocialDtoResponse();
        dto.setId(String.valueOf(obraSocial.getId()));
        dto.setCodigo(obraSocial.getCodigo());
        dto.setNombre(obraSocial.getNombre());
        dto.setActivo(obraSocial.getActivo());
        return dto;
    }
}
