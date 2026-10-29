package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.EspacioOcupadoDtoResponse;
import com.SaludUnificada.Esu.dto.response.EstudioAdjuntoDtoResponse;
import com.SaludUnificada.Esu.entidad.EstudioAdjunto;
import com.SaludUnificada.Esu.servicio.EstudioAdjuntoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @PostMapping(value = "/subir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.canAccessPatient(#pacienteId)")
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
    @PreAuthorize("@securityPolicy.canAccessEstudio(#id)")
    @Operation(summary = "Descarga segura del archivo fisico del estudio medico")
    public ResponseEntity<Resource> descargarEstudio(@PathVariable Long id, Authentication authentication) {
        EstudioAdjunto estudio = estudioServicio.obtenerPorId(id);
        Resource resource = estudioServicio.descargarArchivo(id);

        String contentType = estudio.getMimeType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"estudio_medico_" + id + ".pdf\"")
                .body(resource);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    @Operation(summary = "Listar todos los estudios medicos complementarios de un paciente")
    public ResponseEntity<List<EstudioAdjuntoDtoResponse>> listarPorPaciente(
            @PathVariable Long pacienteId) {
        return ResponseEntity.ok(estudioServicio.listarPorPaciente(pacienteId));
    }

    @GetMapping("/paciente/{pacienteId}/conteo")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    @Operation(summary = "Contar estudios medicos adjuntos de un paciente")
    public ResponseEntity<Long> contarEstudios(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(estudioServicio.contarEstudiosPorPaciente(pacienteId));
    }

    @GetMapping("/paciente/{pacienteId}/espacio")
    @PreAuthorize("@securityPolicy.canAccessPatient(#pacienteId)")
    @Operation(summary = "Consultar volumen de almacenamiento ocupado por estudios del paciente")
    public ResponseEntity<EspacioOcupadoDtoResponse> obtenerEspacioOcupado(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(estudioServicio.obtenerEspacioOcupadoPorPaciente(pacienteId));
    }

    @GetMapping("/evolucion/{evolucionId}")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.canAccessEvolucion(#evolucionId)")
    @Operation(summary = "Listar estudios complementarios vinculados directamente a una evolucion clinica")
    public ResponseEntity<List<EstudioAdjuntoDtoResponse>> listarPorEvolucion(@PathVariable Long evolucionId) {
        return ResponseEntity.ok(estudioServicio.listarPorEvolucion(evolucionId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@securityPolicy.canAccessEstudio(#id)")
    @Operation(summary = "Eliminar de forma permanente un archivo de estudio medico adjunto")
    public ResponseEntity<Void> eliminarEstudio(@PathVariable Long id, Authentication authentication) {
        estudioServicio.eliminarEstudio(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
