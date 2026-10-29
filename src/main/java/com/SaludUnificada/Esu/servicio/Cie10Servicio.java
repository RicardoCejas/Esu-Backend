package com.SaludUnificada.Esu.servicio;

import org.springframework.cache.annotation.Cacheable;

import com.SaludUnificada.Esu.entidad.Cie10;
import com.SaludUnificada.Esu.repositorio.Cie10Repositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Cie10Servicio {

    @Autowired
    private Cie10Repositorio cie10Repositorio;

    @Cacheable(value = "cie10", key = "#query != null ? #query.trim().toLowerCase() : ''")
    public List<Cie10> buscar(String query) {
        if (query == null || query.trim().length() < 2) {
            return cie10Repositorio.findAll(PageRequest.of(0, 20)).getContent();
        }
        return cie10Repositorio.buscarPorCodigoODescripcion(query.trim(), PageRequest.of(0, 30));
    }
}
