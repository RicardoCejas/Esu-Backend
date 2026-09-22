package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.servicio.ITurnoServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
public class TurnoControlador {

    @Autowired
    private ITurnoServicio turnoServicio;

    @PostMapping("/crear")
    public ResponseEntity<TurnoDtoResponse> crearTurno(@Valid @RequestBody TurnoDtoRequest turnoDto) {
        TurnoDtoResponse nuevoTurno = turnoServicio.crearTurno(turnoDto);
        return new ResponseEntity<>(nuevoTurno, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<TurnoDtoResponse> obtenerTurnoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(turnoServicio.obtenerTurnoPorId(id));
    }

    @GetMapping("/obtener/todos")
    public ResponseEntity<List<TurnoDtoResponse>> listarTodos() {
        return ResponseEntity.ok(turnoServicio.listarTodos());
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarTurno(@PathVariable Long id) {
        turnoServicio.eliminarTurno(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filtrar")
    public ResponseEntity<List<TurnoDtoResponse>> filtrarTurnos(
            @RequestParam Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora) {
        return ResponseEntity.ok(turnoServicio.filtrarTurnos(profesionalId, fechaHora));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<TurnoDtoResponse>> obtenerTurnosPorPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(turnoServicio.obtenerTurnosPorPaciente(pacienteId));
    }
}