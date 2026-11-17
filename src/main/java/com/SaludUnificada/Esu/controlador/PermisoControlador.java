package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.entidad.Permiso;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.repositorio.PermisoRepositorio;
import com.SaludUnificada.Esu.seguridad.OwnershipSecurityEvaluator;
import com.SaludUnificada.Esu.seguridad.PermisoGobernanzaPolicy;
import com.SaludUnificada.Esu.servicio.IUsuarioServicio;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private OwnershipSecurityEvaluator securityPolicy;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Listar catalogo de permisos granulares")
    public ResponseEntity<List<Permiso>> listarCatalogo() {
        return ResponseEntity.ok(permisoRepositorio.findAllByOrderByModuloAscNombreAsc());
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.isReceptionistOfUser(#usuarioId)")
    @Operation(summary = "Obtener permisos asignados a un usuario")
    public ResponseEntity<List<String>> obtenerPermisosPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(usuarioServicio.obtenerPermisosUsuario(usuarioId));
    }

    @PutMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.isReceptionistOfUser(#usuarioId)")
    @Operation(summary = "Actualizar permisos de un usuario")
    public ResponseEntity<UsuarioDtoResponse> actualizarPermisosUsuario(
            @PathVariable Long usuarioId,
            @RequestBody List<String> codigosPermisos) {
        List<String> filtrados = codigosPermisos;
        if (securityPolicy != null && !securityPolicy.isCurrentUserAdmin()) {
            securityPolicy.getAuthenticatedUserId().ifPresent(myId -> {
                if (myId.equals(usuarioId)) {
                    throw new ReglaNegocioException(
                            "No podés modificar tus propios permisos desde esta sesión.",
                            HttpStatus.FORBIDDEN
                    );
                }
            });
            if (codigosPermisos != null) {
                filtrados = PermisoGobernanzaPolicy.filterCenterPermissions(codigosPermisos);
            }
        }
        return ResponseEntity.ok(usuarioServicio.actualizarPermisosUsuario(usuarioId, filtrados));
    }
}
