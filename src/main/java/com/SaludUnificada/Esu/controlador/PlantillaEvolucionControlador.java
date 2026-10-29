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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plantillas-evolucion")
@Tag(name = "Plantillas Clínicas", description = "CRUD y Motor de plantillas parametrizables por especialidad (HU-108)")
public class PlantillaEvolucionControlador {

    @Autowired
    private PlantillaEvolucionServicio plantillaServicio;

    @GetMapping
    @Operation(summary = "Listar plantillas de evolución médica (filtrado opcional por especialidadId)")
    public ResponseEntity<List<PlantillaEvolucionDtoResponse>> listarPlantillas(
            @RequestParam(required = false) Long especialidadId) {
        return ResponseEntity.ok(plantillaServicio.listarPlantillas(especialidadId));
    }

    @PostMapping
    @Operation(summary = "Crear nueva plantilla clínica personalizada por especialidad")
    public ResponseEntity<PlantillaEvolucionDtoResponse> crearPlantilla(
            @Valid @RequestBody PlantillaEvolucionDtoRequest request) {
        return new ResponseEntity<>(plantillaServicio.crearPlantilla(request), HttpStatus.CREATED);
    }
    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de plantilla clínica por ID")
    public ResponseEntity<PlantillaEvolucionDtoResponse> obtenerPlantilla(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaServicio.obtenerPorId(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar plantilla clínica por ID")
    public ResponseEntity<Void> desactivarPlantilla(@PathVariable Long id) {
        plantillaServicio.cambiarEstadoPlantilla(id, false);
        return ResponseEntity.noContent().build();
    }
}
