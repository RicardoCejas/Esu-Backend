package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.ObraSocialDtoRequest;
import com.SaludUnificada.Esu.dto.response.ObraSocialDtoResponse;
import com.SaludUnificada.Esu.servicio.ObraSocialServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/obras-sociales")
public class ObraSocialControlador {

    @Autowired
    private ObraSocialServicio obraSocialServicio;

    @GetMapping({"", "/obtener/todas"})
    public ResponseEntity<List<ObraSocialDtoResponse>> listarTodas() {
        return ResponseEntity.ok(obraSocialServicio.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObraSocialDtoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(obraSocialServicio.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ObraSocialDtoResponse> crear(@RequestBody ObraSocialDtoRequest request) {
        return new ResponseEntity<>(obraSocialServicio.crear(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ObraSocialDtoResponse> actualizar(
            @PathVariable Long id,
            @RequestBody ObraSocialDtoRequest request) {
        return ResponseEntity.ok(obraSocialServicio.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        obraSocialServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
