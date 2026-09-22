package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.ProfesionalDtoRequest;
import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.servicio.IProfesionalServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profesionales")
public class ProfesionalControlador {

    @Autowired
    private IProfesionalServicio profesionalServicio;

    @PostMapping("/crear")
    public ResponseEntity<ProfesionalDtoResponse> crearProfesional(@Valid @RequestBody ProfesionalDtoRequest profesionalDto) {
        ProfesionalDtoResponse nuevoProfesional = profesionalServicio.crearProfesional(profesionalDto);
        return new ResponseEntity<>(nuevoProfesional, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<ProfesionalDtoResponse> obtenerProfesionalPorId(@PathVariable Long id) {
        return ResponseEntity.ok(profesionalServicio.obtenerProfesionalPorId(id));
    }

    @GetMapping("/obtener/todos")
    public ResponseEntity<List<ProfesionalDtoResponse>> listarTodos() {
        return ResponseEntity.ok(profesionalServicio.listarTodos());
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarProfesional(@PathVariable Long id) {
        profesionalServicio.eliminarProfesional(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filtrar/especialidad/{especialidadId}")
    public ResponseEntity<List<ProfesionalDtoResponse>> filtrarPorEspecialidad(@PathVariable Long especialidadId) {
        return ResponseEntity.ok(profesionalServicio.filtrarPorEspecialidad(especialidadId));
    }
}
