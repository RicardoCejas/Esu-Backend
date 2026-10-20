package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.ProfesionalDtoRequest;
import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.servicio.IProfesionalServicio;
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
    public ResponseEntity<ProfesionalDtoResponse> crearProfesional(@RequestBody ProfesionalDtoRequest profesionalDto) {
        ProfesionalDtoResponse nuevoProfesional = profesionalServicio.crearProfesional(profesionalDto);
        return new ResponseEntity<>(nuevoProfesional, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<ProfesionalDtoResponse> obtenerProfesionalPorId(@PathVariable Long id) {
        try {
            ProfesionalDtoResponse profesional = profesionalServicio.obtenerProfesionalPorId(id);
            return new ResponseEntity<>(profesional, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/obtener/todos")
    public ResponseEntity<List<ProfesionalDtoResponse>> listarTodos() {
        List<ProfesionalDtoResponse> profesionales = profesionalServicio.listarTodos();
        return new ResponseEntity<>(profesionales, HttpStatus.OK);
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarProfesional(@PathVariable Long id) {
        try {
            profesionalServicio.eliminarProfesional(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/filtrar/especialidad/{especialidadId}")
    public ResponseEntity<List<ProfesionalDtoResponse>> filtrarPorEspecialidad(@PathVariable Long especialidadId) {
        List<ProfesionalDtoResponse> profesionales = profesionalServicio.filtrarPorEspecialidad(especialidadId);
        return new ResponseEntity<>(profesionales, HttpStatus.OK);
    }
}
