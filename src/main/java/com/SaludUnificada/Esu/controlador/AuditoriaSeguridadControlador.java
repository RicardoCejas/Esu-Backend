package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.LogAuditoriaSeguridadDtoResponse;
import com.SaludUnificada.Esu.dto.response.LogAccesoHcDtoResponse;
import com.SaludUnificada.Esu.servicio.AuditoriaSeguridadServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seguridad")
@RequiredArgsConstructor
@Tag(name = "Auditoría de Seguridad", description = "Monitoreo de sesiones, IPs y eventos de autenticación")
public class AuditoriaSeguridadControlador {

    private final AuditoriaSeguridadServicio auditoriaSeguridadServicio;

    @GetMapping("/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener el registro paginado de intentos de login, IPs y User-Agent (Exclusivo ADMIN)")
    public ResponseEntity<Page<LogAuditoriaSeguridadDtoResponse>> listarAuditoria(
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(auditoriaSeguridadServicio.listarAuditoria(pageable));
    }

    @GetMapping("/auditoria/exportar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarAuditoria() {
        byte[] csv = auditoriaSeguridadServicio.exportarAuditoriaCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"auditoria_seguridad.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/auditoria/accesos-clinicos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener el registro de auditoría de accesos clínicos sin revelar contenido médico (Exclusivo ADMIN)")
    public ResponseEntity<Page<LogAccesoHcDtoResponse>> listarAccesosClinicos(
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(auditoriaSeguridadServicio.listarAccesosClinicos(pageable));
    }

}
