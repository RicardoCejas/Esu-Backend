package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.Cie10DtoResponse;
import com.SaludUnificada.Esu.servicio.Cie10Servicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cie10")
@Tag(name = "Catálogo CIE-10", description = "Búsqueda y consulta de diagnósticos estandarizados CIE-10")
public class Cie10Controlador {

    @Autowired
    private Cie10Servicio cie10Servicio;

    @GetMapping
    @Operation(summary = "Consultar catálogo CIE-10")
    public ResponseEntity<List<Cie10DtoResponse>> listarCie10(@RequestParam(value = "q", required = false) String q) {
        return ResponseEntity.ok(mapear(cie10Servicio.buscar(q)));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Búsqueda reactiva de diagnósticos CIE-10")
    public ResponseEntity<List<Cie10DtoResponse>> buscarCie10(@RequestParam(value = "q", required = false) String q) {
        return ResponseEntity.ok(mapear(cie10Servicio.buscar(q)));
    }

    private List<Cie10DtoResponse> mapear(List<com.SaludUnificada.Esu.entidad.Cie10> resultados) {
        return resultados.stream().map(Cie10DtoResponse::desdeEntidad).toList();
    }
}
