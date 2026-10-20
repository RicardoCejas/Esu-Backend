package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.servicio.IPacienteServicio;
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
    public ResponseEntity<PacienteDtoResponse> crearPaciente(@RequestBody PacienteDtoRequest pacienteDto) {
        PacienteDtoResponse nuevoPaciente = pacienteServicio.crearPaciente(pacienteDto);
        return new ResponseEntity<>(nuevoPaciente, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<PacienteDtoResponse> obtenerPacientePorId(@PathVariable Long id) {
        try {
            PacienteDtoResponse paciente = pacienteServicio.obtenerPacientePorId(id);
            return new ResponseEntity<>(paciente, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/obtener/todos")
    public ResponseEntity<List<PacienteDtoResponse>> listarTodos() {
        List<PacienteDtoResponse> pacientes = pacienteServicio.listarTodos();
        return new ResponseEntity<>(pacientes, HttpStatus.OK);
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarPaciente(@PathVariable Long id) {
        try {
            pacienteServicio.eliminarPaciente(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/buscar/dni")
    public ResponseEntity<PacienteDtoResponse> buscarPorDni(@RequestParam String dni) {
        try {
            PacienteDtoResponse paciente = pacienteServicio.buscarPorDni(dni);
            return new ResponseEntity<>(paciente, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/buscar/apellido")
    public ResponseEntity<List<PacienteDtoResponse>> buscarPorApellidoParcial(@RequestParam String apellido) {
        List<PacienteDtoResponse> pacientes = pacienteServicio.buscarPorApellidoParcial(apellido);
        return new ResponseEntity<>(pacientes, HttpStatus.OK);
    }
}
