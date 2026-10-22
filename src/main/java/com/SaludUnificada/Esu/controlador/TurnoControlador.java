package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.CopagoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoCancelacionDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoCheckInDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoEstadoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoLlamadaDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoMostradorDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDisponibleDtoResponse;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.servicio.ITurnoServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
public class TurnoControlador {

    @Autowired
    private ITurnoServicio turnoServicio;

    @PostMapping({"/crear", ""})
    public ResponseEntity<TurnoDtoResponse> crearTurno(@Valid @RequestBody TurnoDtoRequest turnoDto) {
        TurnoDtoResponse nuevoTurno = turnoServicio.crearTurno(turnoDto);
        return new ResponseEntity<>(nuevoTurno, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<TurnoDtoResponse> obtenerTurnoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(turnoServicio.obtenerTurnoPorId(id));
    }

    @GetMapping({"", "/obtener/todos"})
    public ResponseEntity<List<TurnoDtoResponse>> listarTodos() {
        return ResponseEntity.ok(turnoServicio.listarTodos());
    }

    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
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

    @GetMapping("/profesional/{profesionalId}")
    public ResponseEntity<List<TurnoDtoResponse>> obtenerTurnosPorProfesional(
            @PathVariable Long profesionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(turnoServicio.obtenerTurnosPorProfesional(profesionalId, fecha));
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<TurnoDisponibleDtoResponse>> obtenerDisponibles(
            @RequestParam Long profesionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(turnoServicio.obtenerDisponibles(profesionalId, fecha));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN','RECEPCIONISTA')")
    public ResponseEntity<TurnoDtoResponse> actualizarEstado(
            @PathVariable Long id,
            @RequestBody TurnoEstadoDtoRequest request) {
        return ResponseEntity.ok(turnoServicio.actualizarEstado(id, request.getEstado()));
    }

    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN','RECEPCIONISTA')")
    public ResponseEntity<TurnoDtoResponse> registrarCheckIn(
            @PathVariable Long id,
            @RequestBody(required = false) TurnoCheckInDtoRequest request) {
        String hora = request == null ? null : request.getHoraCheckIn();
        return ResponseEntity.ok(turnoServicio.registrarCheckIn(id, hora));
    }

    @PutMapping("/{id}/copago")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    public ResponseEntity<TurnoDtoResponse> registrarCopago(
            @PathVariable Long id,
            @RequestBody CopagoDtoRequest request) {
        return ResponseEntity.ok(turnoServicio.registrarCopago(id, request));
    }

    @PostMapping("/mostrador")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    public ResponseEntity<TurnoDtoResponse> crearTurnoMostrador(@RequestBody TurnoMostradorDtoRequest request) {
        return new ResponseEntity<>(turnoServicio.crearTurnoMostrador(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/cancelar-mostrador")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    public ResponseEntity<TurnoDtoResponse> cancelarConJustificativo(
            @PathVariable Long id,
            @RequestBody TurnoCancelacionDtoRequest request) {
        return ResponseEntity.ok(turnoServicio.cancelarConJustificativo(id, request.getJustificativo()));
    }

    @PutMapping("/{id}/llamar")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN','RECEPCIONISTA')")
    public ResponseEntity<TurnoDtoResponse> llamarPaciente(
            @PathVariable Long id,
            @RequestBody TurnoLlamadaDtoRequest request) {
        return ResponseEntity.ok(turnoServicio.llamarPaciente(
                id,
                request.getConsultorio(),
                request.getHoraLlamado()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelarTurno(@PathVariable Long id) {
        turnoServicio.cancelarTurno(id);
        return ResponseEntity.noContent().build();
    }
}
