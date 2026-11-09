package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.CarnetDigitalDtoResponse;
import com.SaludUnificada.Esu.servicio.CarnetYFamiliarServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/carnet-digital")
@RequiredArgsConstructor
@Tag(name = "Carnet Digital Público", description = "Verificación y consulta pública de credenciales digitales de pacientes sin autenticación")
public class PublicCarnetControlador {

    private final CarnetYFamiliarServicio carnetYFamiliarServicio;

    @GetMapping("/verificar")
    @Operation(summary = "Verificar credencial pública por DNI o ID de paciente")
    public ResponseEntity<?> verificarPublico(
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) String dni) {
        if (pacienteId != null) {
            return ResponseEntity.ok(carnetYFamiliarServicio.emitirCarnetDigital(pacienteId));
        } else if (dni != null && !dni.isBlank()) {
            return ResponseEntity.ok(carnetYFamiliarServicio.emitirCarnetDigitalPorDni(dni));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Debe proporcionar pacienteId o dni"));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Obtener credencial pública por ID de paciente")
    public ResponseEntity<CarnetDigitalDtoResponse> obtenerPublicoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(carnetYFamiliarServicio.emitirCarnetDigital(id));
    }
}
