package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.AlergiaDtoRequest;
import com.SaludUnificada.Esu.dto.response.AlergiaDtoResponse;
import com.SaludUnificada.Esu.servicio.AlergiaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
@Tag(name = "Alergias y Factores de Riesgo", description = "Gestion de alergias criticas y factores de riesgo del paciente")
public class AlergiaControlador {

    private final AlergiaServicio alergiaServicio;

    @PostMapping("/{id}/alergias")
    @PreAuthorize("hasRole('ADMIN') or (hasAnyRole('MEDICO', 'PROFESIONAL') and @securityPolicy.canAccessPatient(#id))")
    @Operation(summary = "Registrar nueva alergia o factor de riesgo clinico para un paciente")
    public ResponseEntity<AlergiaDtoResponse> registrarAlergia(
            @PathVariable Long id,
            @Valid @RequestBody AlergiaDtoRequest request
    ) {
        AlergiaDtoResponse response = alergiaServicio.registrarAlergia(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/alergias")
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    @Operation(summary = "Obtener listado de alergias activas del paciente")
    public ResponseEntity<List<AlergiaDtoResponse>> listarAlergias(
            @PathVariable Long id) {
        return ResponseEntity.ok(alergiaServicio.listarAlergiasPaciente(id));
    }

    @GetMapping("/alergias/{alergiaId}")
    @PreAuthorize("@securityPolicy.canAccessAlergia(#alergiaId)")
    @Operation(summary = "Obtener detalle de una alergia especifica por ID")
    public ResponseEntity<AlergiaDtoResponse> obtenerAlergiaPorId(@PathVariable Long alergiaId) {
        return ResponseEntity.ok(alergiaServicio.obtenerAlergiaPorId(alergiaId));
    }

    @PutMapping("/alergias/{alergiaId}")
    @PreAuthorize("hasRole('ADMIN') or (hasAnyRole('MEDICO', 'PROFESIONAL') and @securityPolicy.canAccessAlergia(#alergiaId))")
    @Operation(summary = "Actualizar los datos de una alergia existente")
    public ResponseEntity<AlergiaDtoResponse> actualizarAlergia(
            @PathVariable Long alergiaId,
            @Valid @RequestBody AlergiaDtoRequest request) {
        return ResponseEntity.ok(alergiaServicio.actualizarAlergia(alergiaId, request));
    }

    @DeleteMapping("/alergias/{alergiaId}")
    @PreAuthorize("hasRole('ADMIN') or (hasAnyRole('MEDICO', 'PROFESIONAL') and @securityPolicy.canAccessAlergia(#alergiaId))")
    @Operation(summary = "Dar de baja o desactivar una alergia registrada")
    public ResponseEntity<Void> eliminarAlergia(@PathVariable Long alergiaId) {
        alergiaServicio.eliminarAlergia(alergiaId);
        return ResponseEntity.noContent().build();
    }
}
