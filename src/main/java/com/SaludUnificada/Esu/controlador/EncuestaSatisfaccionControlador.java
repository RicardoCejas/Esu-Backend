package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.EncuestaSatisfaccionDtoRequest;
import com.SaludUnificada.Esu.dto.response.EncuestaSatisfaccionDtoResponse;
import com.SaludUnificada.Esu.servicio.EncuestaSatisfaccionServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Encuestas de Satisfacción", description = "Evaluación de calidad y satisfacción post-atención médica")
public class EncuestaSatisfaccionControlador {

    private final EncuestaSatisfaccionServicio encuestaServicio;

    @Operation(summary = "Completar encuesta de satisfacción post-atención para un turno")
    @PostMapping("/turnos/{id}/encuesta")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<EncuestaSatisfaccionDtoResponse> registrarEncuesta(
            @PathVariable Long id,
            @Valid @RequestBody EncuestaSatisfaccionDtoRequest request,
            Principal principal) {
        EncuestaSatisfaccionDtoResponse response = encuestaServicio.registrarEncuesta(id, request, principal != null ? principal.getName() : null);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Obtener métricas consolidadas de satisfacción institucional")
    @GetMapping("/admin/metricas/satisfaccion")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'PROFESIONAL')")
    public ResponseEntity<Map<String, Object>> obtenerMetricasSatisfaccion() {
        return ResponseEntity.ok(encuestaServicio.obtenerMetricasSatisfaccion());
    }
}
