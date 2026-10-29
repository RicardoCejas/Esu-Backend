package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.PlantillaEvolucionDtoRequest;
import com.SaludUnificada.Esu.dto.response.PlantillaEvolucionDtoResponse;
import com.SaludUnificada.Esu.servicio.PlantillaEvolucionServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plantillas-evolucion")
@Tag(name = "Plantillas Clinicas", description = "Motor de plantillas clinicas parametrizables por especialidad")
public class PlantillaEvolucionControlador {

    @Autowired
    private PlantillaEvolucionServicio plantillaServicio;

    @GetMapping
    @Operation(summary = "Listar plantillas de evolucion medica")
    public ResponseEntity<List<PlantillaEvolucionDtoResponse>> listarPlantillas(
            @RequestParam(required = false) Long especialidadId) {
        return ResponseEntity.ok(plantillaServicio.listarPlantillas(especialidadId));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar plantillas clinicas por palabra clave en titulo")
    public ResponseEntity<List<PlantillaEvolucionDtoResponse>> buscarPlantillas(
            @RequestParam String termino) {
        return ResponseEntity.ok(plantillaServicio.buscarPorTermino(termino));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Crear nueva plantilla clinica")
    public ResponseEntity<PlantillaEvolucionDtoResponse> crearPlantilla(
            @Valid @RequestBody PlantillaEvolucionDtoRequest request) {
        return new ResponseEntity<>(plantillaServicio.crearPlantilla(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Actualizar plantilla clinica existente")
    public ResponseEntity<PlantillaEvolucionDtoResponse> actualizarPlantilla(
            @PathVariable Long id,
            @Valid @RequestBody PlantillaEvolucionDtoRequest request) {
        return ResponseEntity.ok(plantillaServicio.actualizarPlantilla(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de plantilla clinica por ID")
    public ResponseEntity<PlantillaEvolucionDtoResponse> obtenerPlantilla(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaServicio.obtenerPorId(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Desactivar plantilla clinica por ID")
    public ResponseEntity<Void> desactivarPlantilla(@PathVariable Long id) {
        plantillaServicio.cambiarEstadoPlantilla(id, false);
        return ResponseEntity.noContent().build();
    }
}
