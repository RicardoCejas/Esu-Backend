package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.ConsentimientoDtoRequest;
import com.SaludUnificada.Esu.dto.response.ConsentimientoDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.servicio.ConsentimientoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consentimientos")
@RequiredArgsConstructor
@Tag(name = "Consentimientos Informados", description = "Modulo inalterable de firma y trazabilidad de consentimientos medicos")
public class ConsentimientoControlador {

    private final ConsentimientoServicio consentimientoServicio;

    @Autowired(required = false)
    private PacienteRepositorio pacienteRepositorio;

    private void validarPropiedadPaciente(Long pacienteId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        boolean esPersonal = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                a.getAuthority().equals("ROLE_MEDICO") ||
                a.getAuthority().equals("ROLE_PROFESIONAL"));
        if (!esPersonal && pacienteRepositorio != null) {
            Paciente p = pacienteRepositorio.findByUsuarioEmail(auth.getName()).orElse(null);
            if (p != null && !p.getId().equals(pacienteId)) {
                throw new AccessDeniedException("No posee autorizacion para acceder a consentimientos de otro paciente");
            }
        }
    }

    @PostMapping("/firmar")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Formalizar y registrar digitalmente un consentimiento informado")
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
    public ResponseEntity<List<ConsentimientoDtoResponse>> listarPorPaciente(
            @PathVariable Long id,
            Authentication authentication) {
        validarPropiedadPaciente(id, authentication);
        return ResponseEntity.ok(consentimientoServicio.listarPorPaciente(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Consultar el detalle probatorio y hash de un consentimiento informado por ID")
    public ResponseEntity<ConsentimientoDtoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(consentimientoServicio.obtenerPorId(id));
    }
}
