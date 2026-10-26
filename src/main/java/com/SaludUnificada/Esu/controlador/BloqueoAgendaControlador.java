package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.BloqueoAgendaDtoRequest;
import com.SaludUnificada.Esu.dto.response.BloqueoAgendaDtoResponse;
import com.SaludUnificada.Esu.servicio.IBloqueoAgendaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Bloqueos de Agenda", description = "Endpoints para suspender temporalmente la atención médica por licencias, feriados o vacaciones y reprogramar pacientes")
public class BloqueoAgendaControlador {

    private final IBloqueoAgendaServicio bloqueoAgendaServicio;

    @Operation(
            summary = "Bloquear agenda médica y cancelar preventivamente turnos solapados (HU-BE-90 / HU-FE-62)",
            description = "Registra una indisponibilidad temporal para un profesional y actualiza automáticamente a CANCELADO_POR_MEDICO todos los turnos del rango, retornando la lista de pacientes a reprogramar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bloqueo registrado y turnos cancelados preventivamente",
                    content = @Content(schema = @Schema(implementation = BloqueoAgendaDtoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Fechas inválidas o datos de solicitud incompletos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Profesional no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/api/profesionales/{id}/bloquear-agenda")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<BloqueoAgendaDtoResponse> bloquearAgenda(
            @Parameter(description = "ID del profesional", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody BloqueoAgendaDtoRequest request) {
        BloqueoAgendaDtoResponse response = bloqueoAgendaServicio.registrarBloqueo(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Listar bloqueos de agenda de un profesional",
            description = "Devuelve el historial de indisponibilidades y licencias programadas para el médico solicitado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de bloqueos obtenido"),
            @ApiResponse(responseCode = "404", description = "Profesional no encontrado")
    })
    @GetMapping("/api/profesionales/{id}/bloqueos")
    public ResponseEntity<List<BloqueoAgendaDtoResponse>> listarBloqueosPorProfesional(
            @Parameter(description = "ID del profesional", example = "1")
            @PathVariable Long id) {
        List<BloqueoAgendaDtoResponse> bloqueos = bloqueoAgendaServicio.listarBloqueosPorProfesional(id);
        return ResponseEntity.ok(bloqueos);
    }
}
