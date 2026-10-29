package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.EstudioAdjuntoDtoResponse;
import com.SaludUnificada.Esu.entidad.EstudioAdjunto;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.servicio.EstudioAdjuntoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/estudios")
@RequiredArgsConstructor
@Tag(name = "Estudios Medicos Adjuntos", description = "Almacenamiento y gestion de archivos adjuntos complementarios")
public class EstudioAdjuntoControlador {

    private final EstudioAdjuntoServicio estudioServicio;

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
                throw new AccessDeniedException("No posee autorizacion para acceder a los estudios de otro paciente");
            }
        }
    }

    @PostMapping(value = "/subir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Carga de archivo binario de estudio complementario")
    public ResponseEntity<EstudioAdjuntoDtoResponse> subirEstudio(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("pacienteId") Long pacienteId,
            @RequestParam(value = "evolucionId", required = false) Long evolucionId,
            @RequestParam(value = "tipoEstudio", required = false) String tipoEstudio,
            @RequestParam(value = "fechaEstudio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEstudio
    ) {
        EstudioAdjuntoDtoResponse response = estudioServicio.almacenarEstudio(
                archivo, pacienteId, evolucionId, tipoEstudio, fechaEstudio
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/descargar")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Descarga segura del archivo fisico del estudio medico")
    public ResponseEntity<Resource> descargarEstudio(@PathVariable Long id, Authentication authentication) {
        EstudioAdjunto estudio = estudioServicio.obtenerPorId(id);
        estudioServicio.validarAccesoEstudio(estudio, authentication);
        Resource resource = estudioServicio.descargarArchivo(id);

        String contentType = estudio.getMimeType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + estudio.getNombreArchivo() + "\"")
                .body(resource);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Listar todos los estudios medicos complementarios de un paciente")
    public ResponseEntity<List<EstudioAdjuntoDtoResponse>> listarPorPaciente(
            @PathVariable Long pacienteId,
            Authentication authentication) {
        validarPropiedadPaciente(pacienteId, authentication);
        return ResponseEntity.ok(estudioServicio.listarPorPaciente(pacienteId));
    }

    @GetMapping("/paciente/{pacienteId}/conteo")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Contar estudios medicos adjuntos de un paciente")
    public ResponseEntity<Long> contarEstudios(
            @PathVariable Long pacienteId,
            Authentication authentication) {
        validarPropiedadPaciente(pacienteId, authentication);
        return ResponseEntity.ok(estudioServicio.contarEstudiosPorPaciente(pacienteId));
    }

    @GetMapping("/paciente/{pacienteId}/espacio")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Consultar volumen de almacenamiento ocupado por estudios del paciente")
    public ResponseEntity<Map<String, Object>> obtenerEspacioOcupado(
            @PathVariable Long pacienteId,
            Authentication authentication) {
        validarPropiedadPaciente(pacienteId, authentication);
        return ResponseEntity.ok(estudioServicio.obtenerEspacioOcupadoPorPaciente(pacienteId));
    }

    @GetMapping("/evolucion/{evolucionId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PROFESIONAL', 'ADMIN')")
    @Operation(summary = "Listar estudios complementarios vinculados directamente a una evolucion clinica")
    public ResponseEntity<List<EstudioAdjuntoDtoResponse>> listarPorEvolucion(@PathVariable Long evolucionId) {
        return ResponseEntity.ok(estudioServicio.listarPorEvolucion(evolucionId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'ADMIN')")
    @Operation(summary = "Eliminar de forma permanente un archivo de estudio medico adjunto")
    public ResponseEntity<Void> eliminarEstudio(@PathVariable Long id, Authentication authentication) {
        estudioServicio.eliminarEstudio(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
