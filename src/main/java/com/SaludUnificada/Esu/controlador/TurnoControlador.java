package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;
import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.servicio.ITurnoServicio;
import com.SaludUnificada.Esu.servicio.RecordatorioTurnoServicio;
import com.SaludUnificada.Esu.dto.response.RecordatorioTurnoDtoResponse;
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
import java.time.LocalDate;
import java.security.Principal;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
@Tag(name = "Turnos", description = "Gestión de turnos médicos y demanda espontánea (sobreturnos)")
public class TurnoControlador {

    private final ITurnoServicio turnoServicio;
    private final RecordatorioTurnoServicio recordatorioTurnoServicio;

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
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<TurnoDtoResponse> crearTurno(@Valid @RequestBody TurnoDtoRequest turnoDto, Principal principal) {
        TurnoDtoResponse nuevoTurno = turnoServicio.crearTurno(turnoDto, principal != null ? principal.getName() : null);
        return new ResponseEntity<>(nuevoTurno, HttpStatus.CREATED);
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<LocalDateTime>> obtenerDisponibilidad(
            @RequestParam Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(turnoServicio.obtenerHorariosDisponibles(profesionalId, fecha));
    }

    @GetMapping("/recordatorios")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<RecordatorioTurnoDtoResponse>> listarRecordatorios(Principal principal) {
        return ResponseEntity.ok(recordatorioTurnoServicio.listarParaPaciente(principal.getName()));
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
