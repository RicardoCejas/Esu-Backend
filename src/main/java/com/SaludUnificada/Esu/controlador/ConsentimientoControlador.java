package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.ConsentimientoDtoRequest;
import com.SaludUnificada.Esu.dto.response.ConsentimientoDtoResponse;
import com.SaludUnificada.Esu.servicio.ConsentimientoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consentimientos")
@RequiredArgsConstructor
@Tag(name = "Consentimientos Informados", description = "Módulo inalterable de firma y trazabilidad de consentimientos médicos (Ley 26.529 / HU-103 / HU-75)")
public class ConsentimientoControlador {

    private final ConsentimientoServicio consentimientoServicio;

    @PostMapping("/firmar")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Formalizar y registrar digitalmente un consentimiento informado inmutable con hash SHA-256")
    public ResponseEntity<ConsentimientoDtoResponse> firmarConsentimiento(
            @Valid @RequestBody ConsentimientoDtoRequest request,
            HttpServletRequest servletRequest
    ) {
        String ip = servletRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = servletRequest.getRemoteAddr();
        }
        ConsentimientoDtoResponse response = consentimientoServicio.firmarConsentimiento(request, ip);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/paciente/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Obtener el listado de consentimientos informados asociados a un paciente")
    public ResponseEntity<List<ConsentimientoDtoResponse>> listarPorPaciente(@PathVariable Long id) {
        return ResponseEntity.ok(consentimientoServicio.listarPorPaciente(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Consultar el detalle probatorio y hash de un consentimiento informado por ID")
    public ResponseEntity<ConsentimientoDtoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(consentimientoServicio.obtenerPorId(id));
    }
}
