package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.entidad.Permiso;
import com.SaludUnificada.Esu.repositorio.PermisoRepositorio;
import com.SaludUnificada.Esu.servicio.IUsuarioServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permisos")
@RequiredArgsConstructor
@Tag(name = "Permisos Granulares (PBAC)", description = "Catalogo y asignacion personalizada de permisos a usuarios")
public class PermisoControlador {

    private final PermisoRepositorio permisoRepositorio;
    private final IUsuarioServicio usuarioServicio;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Listar catalogo de permisos granulares")
    public ResponseEntity<List<Permiso>> listarCatalogo() {
        return ResponseEntity.ok(permisoRepositorio.findAllByOrderByModuloAscNombreAsc());
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener permisos asignados a un usuario")
    public ResponseEntity<List<String>> obtenerPermisosPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(usuarioServicio.obtenerPermisosUsuario(usuarioId));
    }

    @PutMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar permisos de un usuario")
    public ResponseEntity<UsuarioDtoResponse> actualizarPermisosUsuario(
            @PathVariable Long usuarioId,
            @RequestBody List<String> codigosPermisos) {
        return ResponseEntity.ok(usuarioServicio.actualizarPermisosUsuario(usuarioId, codigosPermisos));
    }
}
