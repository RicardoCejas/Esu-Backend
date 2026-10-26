package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.entidad.LogAuditoriaSeguridad;
import com.SaludUnificada.Esu.servicio.AuditoriaSeguridadServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seguridad")
@Tag(name = "Auditoría de Seguridad", description = "Monitoreo de sesiones, IPs y eventos de autenticación")
public class AuditoriaSeguridadControlador {

    @Autowired
    private AuditoriaSeguridadServicio auditoriaSeguridadServicio;

    @GetMapping("/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener el registro paginado de intentos de login, IPs y User-Agent (Exclusivo ADMIN)")
    public ResponseEntity<Page<LogAuditoriaSeguridad>> listarAuditoria(
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(auditoriaSeguridadServicio.listarAuditoria(pageable));
    }
}
