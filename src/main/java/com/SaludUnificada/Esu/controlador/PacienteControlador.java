package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.seguridad.AutorizacionServicio;
import com.SaludUnificada.Esu.servicio.IPacienteServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteControlador {

    @Autowired
    private IPacienteServicio pacienteServicio;

    @Autowired
    private AutorizacionServicio autorizacionServicio;

    @PostMapping({"", "/crear"})
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN','RECEPCIONISTA')")
    public ResponseEntity<PacienteDtoResponse> crearPaciente(@Valid @RequestBody PacienteDtoRequest pacienteDto) {
        PacienteDtoResponse nuevoPaciente = pacienteServicio.crearPaciente(pacienteDto);
        return new ResponseEntity<>(nuevoPaciente, HttpStatus.CREATED);
    }

    @GetMapping({"/obtener/{id}", "/{id}"})
    public ResponseEntity<PacienteDtoResponse> obtenerPacientePorId(@PathVariable Long id) {
        autorizacionServicio.verificarAccesoAPaciente(id);
        return ResponseEntity.ok(pacienteServicio.obtenerPacientePorId(id));
    }

    @GetMapping({"", "/obtener/todos"})
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN','RECEPCIONISTA')")
    public ResponseEntity<List<PacienteDtoResponse>> listarTodos() {
        return ResponseEntity.ok(pacienteServicio.listarTodos());
    }

    @DeleteMapping({"/eliminar/{id}", "/{id}"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarPaciente(@PathVariable Long id) {
        pacienteServicio.eliminarPaciente(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar/dni")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN','RECEPCIONISTA')")
    public ResponseEntity<PacienteDtoResponse> buscarPorDni(@RequestParam String dni) {
        return ResponseEntity.ok(pacienteServicio.buscarPorDni(dni));
    }

    @GetMapping("/dni/{dni}")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN','RECEPCIONISTA')")
    public ResponseEntity<PacienteDtoResponse> buscarPorDniEnRuta(@PathVariable String dni) {
        return ResponseEntity.ok(pacienteServicio.buscarPorDni(dni));
    }

    @GetMapping("/buscar/apellido")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN','RECEPCIONISTA')")
    public ResponseEntity<List<PacienteDtoResponse>> buscarPorApellidoParcial(@RequestParam String apellido) {
        return ResponseEntity.ok(pacienteServicio.buscarPorApellidoParcial(apellido));
    }
}
