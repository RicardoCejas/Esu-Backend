package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.DisponibilidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.DisponibilidadDtoResponse;
import com.SaludUnificada.Esu.servicio.IDisponibilidadServicio;
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
@Tag(name = "Disponibilidad y Agendas", description = "Endpoints para la gestión de grilla horaria y cálculo automático de slots para médicos")
public class DisponibilidadControlador {

    private final IDisponibilidadServicio disponibilidadServicio;

    @Operation(
            summary = "Configurar disponibilidad horaria de un profesional y generar slots (HU-BE-89)",
            description = "Registra los días y franjas horarias de atención de un médico, validando solapamientos y generando automáticamente los cupos libres para los próximos 30 días sin duplicados ni solapamiento con feriados o bloqueos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Disponibilidad configurada exitosamente y cupos generados",
                    content = @Content(schema = @Schema(implementation = DisponibilidadDtoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Rango horario inválido o datos incorrectos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Solapamiento con otra franja horaria activa del profesional",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Profesional no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/api/disponibilidades")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<DisponibilidadDtoResponse> configurarDisponibilidad(
            @Valid @RequestBody DisponibilidadDtoRequest request) {
        DisponibilidadDtoResponse response = disponibilidadServicio.configurarDisponibilidad(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Obtener grilla horaria activa de un profesional (HU-BE-89 / HU-FE-61)",
            description = "Retorna el listado de días, franjas horarias e intervalos de consulta configurados para el profesional solicitado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de disponibilidades activas obtenido"),
            @ApiResponse(responseCode = "404", description = "Profesional no encontrado")
    })
    @GetMapping({"/api/profesionales/{id}/disponibilidad", "/api/disponibilidades/profesional/{id}"})
    public ResponseEntity<List<DisponibilidadDtoResponse>> obtenerDisponibilidadPorProfesional(
            @Parameter(description = "ID del profesional", example = "1")
            @PathVariable Long id) {
        List<DisponibilidadDtoResponse> response = disponibilidadServicio.obtenerDisponibilidadPorProfesional(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Regenerar slots de turnos para los próximos 30 días",
            description = "Ejecuta el recálculo y persistencia de turnos libres para los próximos 30 días omitiendo feriados y bloqueos."
    )
    @PostMapping("/api/disponibilidades/profesional/{id}/generar-slots")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<Integer> generarSlotsProximos30Dias(@PathVariable Long id) {
        int slotsGenerados = disponibilidadServicio.generarSlotsProximos30Dias(id);
        return ResponseEntity.ok(slotsGenerados);
    }
}
