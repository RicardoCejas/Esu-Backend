package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.ConsentimientoDtoRequest;
import com.SaludUnificada.Esu.dto.response.ConsentimientoDtoResponse;
import com.SaludUnificada.Esu.seguridad.RequestIpUtil;
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
import java.util.Map;

@RestController
@RequestMapping("/api/consentimientos")
@RequiredArgsConstructor
@Tag(name = "Consentimientos Informados", description = "Modulo inalterable de firma y trazabilidad de consentimientos medicos")
public class ConsentimientoControlador {

    private final ConsentimientoServicio consentimientoServicio;
    private final RequestIpUtil requestIpUtil;

    @PostMapping("/firmar")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.canAccessPatient(#request.pacienteId)")
    @Operation(summary = "Formalizar y registrar digitalmente un consentimiento informado")
    public ResponseEntity<ConsentimientoDtoResponse> firmarConsentimiento(
            @Valid @RequestBody ConsentimientoDtoRequest request,
            HttpServletRequest servletRequest
    ) {
        String ip = requestIpUtil.obtenerIpCliente(servletRequest);
        ConsentimientoDtoResponse response = consentimientoServicio.firmarConsentimiento(request, ip);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/paciente/{id}")
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
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

    @PatchMapping("/{id}/revocar")
    @PreAuthorize("hasAnyRole('PACIENTE', 'ADMIN', 'PROFESIONAL', 'MEDICO')")
    public ResponseEntity<ConsentimientoDtoResponse> revocarConsentimiento(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload,
            HttpServletRequest request
    ) {
        String motivo = (payload != null) ? payload.get("motivo") : null;
        String ip = requestIpUtil.obtenerIpCliente(request);
        return ResponseEntity.ok(consentimientoServicio.revocarConsentimiento(id, motivo, ip));
    }

    @GetMapping("/profesional/{profesionalId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<List<ConsentimientoDtoResponse>> listarPorProfesional(@PathVariable Long profesionalId) {
        return ResponseEntity.ok(consentimientoServicio.listarPorProfesional(profesionalId));
    }
}
