package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.servicio.ITurnoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
@Tag(name = "Turnos", description = "Gestión de turnos médicos y demanda espontánea (sobreturnos)")
public class TurnoControlador {

    private final ITurnoServicio turnoServicio;

    @Operation(
            summary = "Registrar sobreturno médico de urgencia en sala (HU-BE-91 / HU-FE-63)",
            description = "Permite registrar un sobreturno para atención de urgencias menores con flag esSobreturno=true, validando que no se supere el cupo máximo diario (3 sobreturnos por médico)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sobreturno registrado exitosamente",
                    content = @Content(schema = @Schema(implementation = TurnoDtoResponse.class))),
            @ApiResponse(responseCode = "422", description = "Cupo máximo de sobreturnos diarios superado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Profesional o paciente no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: requiere rol MEDICO o RECEPCIONISTA")
    })
    @PostMapping("/sobreturno")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'RECEPCIONISTA', 'ADMIN')")
    public ResponseEntity<TurnoDtoResponse> crearSobreturno(@Valid @RequestBody SobreturnoDtoRequest request) {
        TurnoDtoResponse nuevoTurno = turnoServicio.crearSobreturno(request);
        return new ResponseEntity<>(nuevoTurno, HttpStatus.CREATED);
    }

    @PostMapping("/crear")
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
