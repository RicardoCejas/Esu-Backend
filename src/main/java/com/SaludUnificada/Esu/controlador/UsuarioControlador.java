package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.PreferenciasUsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.request.UsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.request.UsuarioPerfilOperativoDtoRequest;
import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.seguridad.OwnershipSecurityEvaluator;
import com.SaludUnificada.Esu.seguridad.PermisoGobernanzaPolicy;
import com.SaludUnificada.Esu.servicio.CarnetYFamiliarServicio;
import com.SaludUnificada.Esu.servicio.IUsuarioServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioControlador {

    private static final Set<String> ROLES_OPERATIVOS_CENTRO = Set.of("RECEPCIONISTA", "MEDICO", "PROFESIONAL");

    private final IUsuarioServicio usuarioServicio;
    private final CarnetYFamiliarServicio carnetYFamiliarServicio;

    @Autowired(required = false)
    private OwnershipSecurityEvaluator securityPolicy;

    private boolean esAdministrador() {
        return securityPolicy == null || securityPolicy.isCurrentUserAdmin();
    }

    private String normalizarRolPermitidoCentro(String rawRol) {
        String limpio = rawRol == null ? "" : rawRol.trim().toUpperCase().replaceFirst("^ROLE_", "");
        if (limpio.isEmpty()) {
            return "RECEPCIONISTA";
        }
        if (!ROLES_OPERATIVOS_CENTRO.contains(limpio)) {
            throw new AccessDeniedException("Un centro de salud solo puede gestionar cuentas de recepción o profesionales médicos de su sede.");
        }
        return "MEDICO".equals(limpio) ? "PROFESIONAL" : limpio;
    }

    @PostMapping({"", "/crear"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<UsuarioDtoResponse> crearUsuario(@Valid @RequestBody UsuarioDtoRequest usuarioDto) {
        if (!esAdministrador()) {
            Long miCentroId = securityPolicy.getAuthenticatedCentroSaludIdOrThrow();
            String rolNormalizado = normalizarRolPermitidoCentro(usuarioDto.getRol());
            usuarioDto.setRol(rolNormalizado);
            usuarioDto.setCentroSaludId(miCentroId);
            securityPolicy.getAuthenticatedCentroSaludNombre().ifPresent(usuarioDto::setCentroSaludNombre);
            usuarioDto.setPermisos(
                    PermisoGobernanzaPolicy.resolveCenterCreatePermissions(rolNormalizado, usuarioDto.getPermisos())
            );
        }
        UsuarioDtoResponse nuevoUsuario = usuarioServicio.crearUsuario(usuarioDto);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.isReceptionistOfUser(#id)")
    public ResponseEntity<UsuarioDtoResponse> obtenerUsuarioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioServicio.obtenerUsuarioPorId(id));
    }

    @GetMapping({"", "/obtener/todos"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<UsuarioDtoResponse>> listarTodos() {
        if (esAdministrador()) {
            return ResponseEntity.ok(usuarioServicio.listarTodos());
        }
        Long miCentroId = securityPolicy.getAuthenticatedCentroSaludIdOrThrow();
        return ResponseEntity.ok(usuarioServicio.listarPorCentro(miCentroId));
    }

    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.isReceptionistOfUser(#id)")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        if (!esAdministrador()) {
            securityPolicy.getAuthenticatedUserId().ifPresent(myId -> {
                if (myId.equals(id)) {
                    throw new ReglaNegocioException("No podés eliminar tu propia cuenta en sesión.");
                }
            });
        }
        usuarioServicio.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar/username")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioDtoResponse> buscarPorUsername(@RequestParam String username) {
        return ResponseEntity.ok(usuarioServicio.findByUsername(username));
    }

    @GetMapping("/filtrar/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioDtoResponse>> filtrarPorEstado(@RequestParam boolean estado) {
        return ResponseEntity.ok(usuarioServicio.findByActivo(estado));
    }

    @PatchMapping("/preferencias")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> actualizarPreferencias(
            Principal principal,
            @RequestBody PreferenciasUsuarioDtoRequest request) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        carnetYFamiliarServicio.actualizarPreferencias(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.isReceptionistOfUser(#id)")
    public ResponseEntity<UsuarioDtoResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean activo,
            @RequestBody(required = false) Map<String, Object> body) {
        boolean nuevoEstado = activo != null
                ? activo
                : (body != null && body.get("activo") instanceof Boolean b ? b : true);
        if (!esAdministrador() && !nuevoEstado) {
            securityPolicy.getAuthenticatedUserId().ifPresent(myId -> {
                if (myId.equals(id)) {
                    throw new ReglaNegocioException("No podés desactivar tu propia cuenta en sesión.");
                }
            });
        }
        return ResponseEntity.ok(usuarioServicio.cambiarEstadoActivo(id, nuevoEstado));
    }

    @PostMapping("/me/revocar-sesiones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> revocarMisSesiones(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        usuarioServicio.revocarSesionesPorEmail(principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/asignacion")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.isReceptionistOfUser(#id)")
    public ResponseEntity<UsuarioDtoResponse> actualizarAsignacion(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String rol = body != null && body.get("rol") instanceof String r ? r : null;
        Long centroId = null;
        if (body != null && body.get("centroSaludId") != null) {
            try {
                centroId = Long.valueOf(body.get("centroSaludId").toString());
            } catch (NumberFormatException ignored) {}
        }
        String centroNombre = body != null && body.get("centroSaludNombre") instanceof String cn ? cn : null;
        if (!esAdministrador()) {
            securityPolicy.getAuthenticatedUserId().ifPresent(myId -> {
                if (myId.equals(id)) {
                    throw new ReglaNegocioException(
                            "No podés modificar tu propio rol ni sede desde esta sesión.",
                            HttpStatus.FORBIDDEN
                    );
                }
            });
            if (rol != null && !rol.isBlank()) {
                rol = normalizarRolPermitidoCentro(rol);
            }
            centroId = securityPolicy.getAuthenticatedCentroSaludIdOrThrow();
            centroNombre = securityPolicy.getAuthenticatedCentroSaludNombre().orElse(centroNombre);
        }
        return ResponseEntity.ok(usuarioServicio.actualizarRolYCentro(id, rol, centroId, centroNombre));
    }

    @PutMapping("/{id}/perfil-operativo")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.isReceptionistOfUser(#id)")
    public ResponseEntity<UsuarioDtoResponse> actualizarPerfilOperativo(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioPerfilOperativoDtoRequest request) {
        return ResponseEntity.ok(usuarioServicio.actualizarPerfilOperativo(id, request));
    }

    @PostMapping("/{id}/revocar-sesiones")
    @PreAuthorize("hasRole('ADMIN') or @securityPolicy.isReceptionistOfUser(#id)")
    public ResponseEntity<Void> revocarSesionesUsuario(@PathVariable Long id) {
        usuarioServicio.revocarSesiones(id);
        return ResponseEntity.noContent().build();
    }
}
