package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.HistoriaClinicaDtoRequest;
import com.SaludUnificada.Esu.dto.response.HistoriaClinicaDtoResponse;
import com.SaludUnificada.Esu.servicio.IHistoriaClinicaServicio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/historias-clinicas")
@RequiredArgsConstructor
public class HistoriaClinicaControlador {

    private final IHistoriaClinicaServicio historiaClinicaServicio;

    @PostMapping("/crear")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<HistoriaClinicaDtoResponse> crearHistoriaClinica(@Valid @RequestBody HistoriaClinicaDtoRequest dto) {
        HistoriaClinicaDtoResponse nuevaHistoria = historiaClinicaServicio.crearHistoriaClinica(dto);
        return new ResponseEntity<>(nuevaHistoria, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    @PreAuthorize("@securityPolicy.canAccessHistoriaClinica(#id)")
    public ResponseEntity<HistoriaClinicaDtoResponse> obtenerHistoriaClinicaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(historiaClinicaServicio.obtenerHistoriaClinicaPorId(id));
    }

    @GetMapping("/obtener/todos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<HistoriaClinicaDtoResponse>> listarTodas(Pageable pageable) {
        return ResponseEntity.ok(historiaClinicaServicio.listarTodasPaginado(pageable));
    }

    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarHistoriaClinica(@PathVariable Long id) {
        historiaClinicaServicio.eliminarHistoriaClinica(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    public ResponseEntity<List<HistoriaClinicaDtoResponse>> buscarPorPacienteId(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(historiaClinicaServicio.buscarPorPacienteId(pacienteId));
    }

    @GetMapping("/rango")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<HistoriaClinicaDtoResponse>> buscarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            Pageable pageable) {
        return ResponseEntity.ok(historiaClinicaServicio.buscarPorRangoFechasPaginado(inicio, fin, pageable));
    }

    @GetMapping("/paciente/{pacienteId}/conteo")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    public ResponseEntity<Long> contarPorPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(historiaClinicaServicio.contarPorPaciente(pacienteId));
    }

    @GetMapping("/paciente/{pacienteId}/paginado")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    public ResponseEntity<Page<HistoriaClinicaDtoResponse>> listarPorPacientePaginado(
            @PathVariable Long pacienteId,
            Pageable pageable) {
        return ResponseEntity.ok(historiaClinicaServicio.listarPorPacientePaginado(pacienteId, pageable));
    }

    @PostMapping("/emergencia/{pacienteId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<java.util.Map<String, String>> habilitarAccesoEmergencia(
            @PathVariable Long pacienteId,
            @RequestBody java.util.Map<String, String> payload,
            java.security.Principal principal,
            HttpServletRequest request) {
        String motivo = payload != null ? payload.get("motivo") : null;
        historiaClinicaServicio.registrarAccesoEmergencia(pacienteId, principal != null ? principal.getName() : null, motivo, request);
        return ResponseEntity.ok(java.util.Map.of(
                "mensaje", "Acceso de emergencia/guardia registrado y habilitado exitosamente bajo protocolo de auditoría médica legal."
        ));
    }
}