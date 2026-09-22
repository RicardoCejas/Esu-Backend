package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.servicio.IPacienteServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteControlador {

    @Autowired
    private IPacienteServicio pacienteServicio;

    @PostMapping("/crear")
    public ResponseEntity<PacienteDtoResponse> crearPaciente(@Valid @RequestBody PacienteDtoRequest pacienteDto) {
        PacienteDtoResponse nuevoPaciente = pacienteServicio.crearPaciente(pacienteDto);
        return new ResponseEntity<>(nuevoPaciente, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<PacienteDtoResponse> obtenerPacientePorId(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteServicio.obtenerPacientePorId(id));
    }

    @GetMapping("/obtener/todos")
    public ResponseEntity<List<PacienteDtoResponse>> listarTodos() {
        return ResponseEntity.ok(pacienteServicio.listarTodos());
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarPaciente(@PathVariable Long id) {
        pacienteServicio.eliminarPaciente(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar/dni")
    public ResponseEntity<PacienteDtoResponse> buscarPorDni(@RequestParam String dni) {
        return ResponseEntity.ok(pacienteServicio.buscarPorDni(dni));
    }

    @GetMapping("/buscar/apellido")
    public ResponseEntity<List<PacienteDtoResponse>> buscarPorApellidoParcial(@RequestParam String apellido) {
        return ResponseEntity.ok(pacienteServicio.buscarPorApellidoParcial(apellido));
    }
}
