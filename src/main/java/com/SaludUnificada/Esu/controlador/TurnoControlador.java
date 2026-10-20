package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.servicio.ITurnoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
public class TurnoControlador {

    @Autowired
    private ITurnoServicio turnoServicio;

    @PostMapping("/crear")
    public ResponseEntity<TurnoDtoResponse> crearTurno(@RequestBody TurnoDtoRequest turnoDto) {
        try {
            TurnoDtoResponse nuevoTurno = turnoServicio.crearTurno(turnoDto);
            return new ResponseEntity<>(nuevoTurno, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<TurnoDtoResponse> obtenerTurnoPorId(@PathVariable Long id) {
        try {
            TurnoDtoResponse turno = turnoServicio.obtenerTurnoPorId(id);
            return new ResponseEntity<>(turno, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/obtener/todos")
    public ResponseEntity<List<TurnoDtoResponse>> listarTodos() {
        List<TurnoDtoResponse> turnos = turnoServicio.listarTodos();
        return new ResponseEntity<>(turnos, HttpStatus.OK);
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarTurno(@PathVariable Long id) {
        try {
            turnoServicio.eliminarTurno(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/filtrar")
    public ResponseEntity<List<TurnoDtoResponse>> filtrarTurnos(
            @RequestParam Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<TurnoDtoResponse> turnos = turnoServicio.filtrarTurnos(profesionalId, fecha);
        return new ResponseEntity<>(turnos, HttpStatus.OK);
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<TurnoDtoResponse>> obtenerTurnosPorPaciente(@PathVariable Long pacienteId) {
        List<TurnoDtoResponse> turnos = turnoServicio.obtenerTurnosPorPaciente(pacienteId);
        return new ResponseEntity<>(turnos, HttpStatus.OK);
    }
}
