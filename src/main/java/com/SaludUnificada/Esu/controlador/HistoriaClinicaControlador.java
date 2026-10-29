package com.SaludUnificada.Esu.controlador;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.SaludUnificada.Esu.dto.request.HistoriaClinicaDtoRequest;
import com.SaludUnificada.Esu.dto.response.HistoriaClinicaDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.servicio.IHistoriaClinicaServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/historias-clinicas")
public class HistoriaClinicaControlador {

    @Autowired
    private IHistoriaClinicaServicio historiaClinicaServicio;

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
                throw new AccessDeniedException("No posee autorizacion para acceder a la historia clinica de otro paciente");
            }
        }
    }

    @PostMapping("/crear")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<HistoriaClinicaDtoResponse> crearHistoriaClinica(@Valid @RequestBody HistoriaClinicaDtoRequest dto) {
        HistoriaClinicaDtoResponse nuevaHistoria = historiaClinicaServicio.crearHistoriaClinica(dto);
        return new ResponseEntity<>(nuevaHistoria, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<HistoriaClinicaDtoResponse> obtenerHistoriaClinicaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(historiaClinicaServicio.obtenerHistoriaClinicaPorId(id));
    }

    @GetMapping("/obtener/todos")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<List<HistoriaClinicaDtoResponse>> listarTodas() {
        return ResponseEntity.ok(historiaClinicaServicio.listarTodas());
    }

    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarHistoriaClinica(@PathVariable Long id) {
        historiaClinicaServicio.eliminarHistoriaClinica(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<List<HistoriaClinicaDtoResponse>> buscarPorPacienteId(
            @PathVariable Long pacienteId,
            Authentication authentication) {
        validarPropiedadPaciente(pacienteId, authentication);
        return ResponseEntity.ok(historiaClinicaServicio.buscarPorPacienteId(pacienteId));
    }

    @GetMapping("/rango")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<List<HistoriaClinicaDtoResponse>> buscarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(historiaClinicaServicio.buscarPorRangoFechas(inicio, fin));
    }
    @GetMapping("/paciente/{pacienteId}/conteo")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<Long> contarPorPaciente(@PathVariable Long pacienteId, Authentication authentication) {
        validarPropiedadPaciente(pacienteId, authentication);
        return ResponseEntity.ok(historiaClinicaServicio.contarPorPaciente(pacienteId));
    }

    @GetMapping("/paciente/{pacienteId}/paginado")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<Page<HistoriaClinicaDtoResponse>> listarPorPacientePaginado(
            @PathVariable Long pacienteId,
            Pageable pageable,
            Authentication authentication) {
        validarPropiedadPaciente(pacienteId, authentication);
        return ResponseEntity.ok(historiaClinicaServicio.listarPorPacientePaginado(pacienteId, pageable));
    }
}
