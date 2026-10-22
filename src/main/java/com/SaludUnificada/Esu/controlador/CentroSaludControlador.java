package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.CentroSaludDtoRequest;
import com.SaludUnificada.Esu.dto.response.CentroSaludDtoResponse;
import com.SaludUnificada.Esu.servicio.CentroSaludServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/centros-salud")
public class CentroSaludControlador {

    @Autowired
    private CentroSaludServicio centroSaludServicio;

    @GetMapping({"", "/obtener/todos"})
    public ResponseEntity<List<CentroSaludDtoResponse>> listarTodos() {
        return ResponseEntity.ok(centroSaludServicio.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CentroSaludDtoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(centroSaludServicio.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<CentroSaludDtoResponse> crear(@RequestBody CentroSaludDtoRequest request) {
        return new ResponseEntity<>(centroSaludServicio.crear(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CentroSaludDtoResponse> actualizar(
            @PathVariable Long id,
            @RequestBody CentroSaludDtoRequest request) {
        return ResponseEntity.ok(centroSaludServicio.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        centroSaludServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
