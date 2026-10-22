package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.servicio.IProfesionalServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicos")
public class MedicoControlador {

    @Autowired
    private IProfesionalServicio profesionalServicio;

    @GetMapping({"", "/obtener/todos"})
    public ResponseEntity<List<ProfesionalDtoResponse>> listarTodos() {
        return ResponseEntity.ok(profesionalServicio.listarTodos());
    }

    @GetMapping({"/{id}", "/obtener/{id}"})
    public ResponseEntity<ProfesionalDtoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(profesionalServicio.obtenerProfesionalPorId(id));
    }

    @GetMapping({"/especialidad/{especialidadId}", "/filtrar/especialidad/{especialidadId}"})
    public ResponseEntity<List<ProfesionalDtoResponse>> filtrarPorEspecialidad(@PathVariable Long especialidadId) {
        return ResponseEntity.ok(profesionalServicio.filtrarPorEspecialidad(especialidadId));
    }

    @GetMapping("/centro/{centroSaludId}")
    public ResponseEntity<List<ProfesionalDtoResponse>> filtrarPorCentro(@PathVariable Long centroSaludId) {
        return ResponseEntity.ok(profesionalServicio.filtrarPorCentro(centroSaludId));
    }
}
