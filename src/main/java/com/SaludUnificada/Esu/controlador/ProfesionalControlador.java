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

    @PostMapping("/crear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfesionalDtoResponse> crearProfesional(@Valid @RequestBody ProfesionalDtoRequest profesionalDto) {
        ProfesionalDtoResponse nuevoProfesional = profesionalServicio.crearProfesional(profesionalDto);
        return new ResponseEntity<>(nuevoProfesional, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<ProfesionalDtoResponse> obtenerProfesionalPorId(@PathVariable Long id) {
        return ResponseEntity.ok(profesionalServicio.obtenerProfesionalPorId(id));
    }

    @GetMapping({"", "/obtener/todos"})
    public ResponseEntity<List<ProfesionalDtoResponse>> listarTodos() {
        return ResponseEntity.ok(profesionalServicio.listarTodos());
    }

    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarProfesional(@PathVariable Long id) {
        profesionalServicio.eliminarProfesional(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filtrar/especialidad/{especialidadId}")
    public ResponseEntity<List<ProfesionalDtoResponse>> filtrarPorEspecialidad(@PathVariable Long especialidadId) {
        return ResponseEntity.ok(profesionalServicio.filtrarPorEspecialidad(especialidadId));
    }
}
