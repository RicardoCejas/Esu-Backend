package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.AlergiaDtoRequest;
import com.SaludUnificada.Esu.dto.response.AlergiaDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.servicio.AlergiaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
@Tag(name = "Alergias y Factores de Riesgo", description = "Gestion de alergias criticas y factores de riesgo del paciente")
public class AlergiaControlador {

    private final AlergiaServicio alergiaServicio;

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
                throw new AccessDeniedException("No posee autorizacion para acceder al registro de alergias de otro paciente");
            }
        }
    }

    @PostMapping("/{id}/alergias")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Registrar nueva alergia o factor de riesgo clinico para un paciente")
    public ResponseEntity<AlergiaDtoResponse> registrarAlergia(
            @PathVariable Long id,
            @Valid @RequestBody AlergiaDtoRequest request
    ) {
        AlergiaDtoResponse response = alergiaServicio.registrarAlergia(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/alergias")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Obtener listado de alergias activas del paciente")
    public ResponseEntity<List<AlergiaDtoResponse>> listarAlergias(
            @PathVariable Long id,
            Authentication authentication) {
        validarPropiedadPaciente(id, authentication);
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
