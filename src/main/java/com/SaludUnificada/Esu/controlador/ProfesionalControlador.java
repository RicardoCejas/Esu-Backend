package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.ProfesionalDtoRequest;
import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.servicio.IProfesionalServicio;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/profesionales")
@RequiredArgsConstructor
@Tag(name = "Profesionales", description = "Gestión de prestadores médicos, matrículas, especialidades y búsqueda de turnos disponibles")
public class ProfesionalControlador {

    private final IProfesionalServicio profesionalServicio;

    @Operation(
            summary = "Búsqueda optimizada de prestadores por especialidad, centro de salud y primeros cupos libres (HU-BE-92 / HU-FE-64)",
            description = "Consulta de alto rendimiento optimizada con JOIN FETCH que filtra profesionales activos por especialidad y centro de salud, incluyendo la fecha y hora del próximo turno libre disponible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de profesionales disponibles obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = ProfesionalDtoResponse.class)))
    })
    @GetMapping("/buscar-disponibles")
    public ResponseEntity<List<ProfesionalDtoResponse>> buscarDisponibles(
            @Parameter(description = "ID opcional de la especialidad", example = "1")
            @RequestParam(required = false) Long especialidadId,
            @Parameter(description = "ID opcional de la sede o centro médico", example = "1")
            @RequestParam(required = false) Long centroSaludId,
            @Parameter(description = "Filtrar únicamente profesionales con al menos un turno disponible", example = "true")
            @RequestParam(required = false, defaultValue = "false") Boolean soloConDisponibilidad) {
        List<ProfesionalDtoResponse> resultado = profesionalServicio.buscarDisponibles(especialidadId, centroSaludId, soloConDisponibilidad);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping({"", "/crear"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Registrar nuevo profesional médico")
    public ResponseEntity<ProfesionalDtoResponse> crearProfesional(@Valid @RequestBody ProfesionalDtoRequest profesionalDto) {
        ProfesionalDtoResponse nuevoProfesional = profesionalServicio.crearProfesional(profesionalDto);
        return new ResponseEntity<>(nuevoProfesional, HttpStatus.CREATED);
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Actualizar datos de un profesional médico")
    public ResponseEntity<ProfesionalDtoResponse> actualizarProfesional(
            @PathVariable Long id,
            @Valid @RequestBody ProfesionalDtoRequest profesionalDto) {
        return ResponseEntity.ok(profesionalServicio.actualizarProfesional(id, profesionalDto));
    }

    @GetMapping({"/{id:\\d+}", "/obtener/{id:\\d+}"})
    @Operation(summary = "Obtener profesional por ID")
    public ResponseEntity<ProfesionalDtoResponse> obtenerProfesionalPorId(@PathVariable Long id) {
        return ResponseEntity.ok(profesionalServicio.obtenerProfesionalPorId(id));
    }

    @GetMapping({"", "/obtener/todos"})
    @Operation(summary = "Listar todos los profesionales médicos")
    public ResponseEntity<List<ProfesionalDtoResponse>> listarTodos() {
        return ResponseEntity.ok(profesionalServicio.listarTodos());
    }

    @DeleteMapping({"/{id:\\d+}", "/eliminar/{id:\\d+}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Eliminar profesional médico")
    public ResponseEntity<Void> eliminarProfesional(@PathVariable Long id) {
        profesionalServicio.eliminarProfesional(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/especialidad/{especialidadId:\\d+}", "/filtrar/especialidad/{especialidadId:\\d+}"})
    @Operation(summary = "Filtrar profesionales por especialidad")
    public ResponseEntity<List<ProfesionalDtoResponse>> filtrarPorEspecialidad(@PathVariable Long especialidadId) {
        return ResponseEntity.ok(profesionalServicio.filtrarPorEspecialidad(especialidadId));
    }

    @GetMapping("/centro/{centroId:\\d+}")
    @Operation(summary = "Filtrar profesionales adscriptos a un centro de salud")
    public ResponseEntity<List<ProfesionalDtoResponse>> filtrarPorCentro(@PathVariable Long centroId) {
        return ResponseEntity.ok(profesionalServicio.filtrarPorCentro(centroId));
    }

    @PostMapping("/{id:\\d+}/centros/{centroId:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Vincular profesional a un centro de salud")
    public ResponseEntity<Set<Long>> vincularCentro(@PathVariable Long id, @PathVariable Long centroId) {
        return ResponseEntity.ok(profesionalServicio.vincularCentro(id, centroId));
    }

    @DeleteMapping("/{id:\\d+}/centros/{centroId:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Desvincular profesional de un centro de salud")
    public ResponseEntity<Set<Long>> desvincularCentro(@PathVariable Long id, @PathVariable Long centroId) {
        return ResponseEntity.ok(profesionalServicio.desvincularCentro(id, centroId));
    }

    @PatchMapping("/{id:\\d+}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Activar o desactivar un profesional médico")
    public ResponseEntity<ProfesionalDtoResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo) {
        return ResponseEntity.ok(profesionalServicio.cambiarEstadoActivo(id, activo));
    }
}
