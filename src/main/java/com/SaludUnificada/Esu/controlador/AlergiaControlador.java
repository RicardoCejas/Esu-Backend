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
@Tag(name = "Alergias y Factores de Riesgo", description = "Gestión de alergias críticas y factores de riesgo del paciente (HU-104 / HU-76)")
public class AlergiaControlador {

    private final AlergiaServicio alergiaServicio;

    @PostMapping("/{id}/alergias")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Registrar nueva alergia o factor de riesgo clínico para un paciente (HU-104)")
    public ResponseEntity<AlergiaDtoResponse> registrarAlergia(
            @PathVariable Long id,
            @Valid @RequestBody AlergiaDtoRequest request
    ) {
        AlergiaDtoResponse response = alergiaServicio.registrarAlergia(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/alergias")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Obtener listado de alergias activas del paciente para banners y alertas (HU-104)")
    public ResponseEntity<List<AlergiaDtoResponse>> listarAlergias(@PathVariable Long id) {
        return ResponseEntity.ok(alergiaServicio.listarAlergiasPaciente(id));
    }

    @DeleteMapping("/alergias/{alergiaId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Dar de baja o desactivar una alergia registrada")
    public ResponseEntity<Void> eliminarAlergia(@PathVariable Long alergiaId) {
        alergiaServicio.eliminarAlergia(alergiaId);
        return ResponseEntity.noContent().build();
    }
}
