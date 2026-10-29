package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.NotificacionDtoResponse;
import com.SaludUnificada.Esu.servicio.NotificacionUsuarioServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones In-App", description = "Buzón y gestión de notificaciones y alertas internas para el usuario")
public class NotificacionControlador {

    private final NotificacionUsuarioServicio notificacionUsuarioServicio;

    @Operation(summary = "Listar notificaciones del usuario autenticado")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificacionDtoResponse>> listarNotificaciones(@RequestParam(name = "soloNoLeidas", required = false) Boolean soloNoLeidas, Principal principal) {
        return ResponseEntity.ok(notificacionUsuarioServicio.listarParaUsuario(principal.getName(), soloNoLeidas));
    }

    @Operation(summary = "Contar notificaciones no leídas para el badge/campanita")
    @GetMapping("/no-leidas/contar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(Principal principal) {
        long count = notificacionUsuarioServicio.contarNoLeidas(principal.getName());
        return ResponseEntity.ok(Map.of("noLeidas", count));
    }

    @Operation(summary = "Marcar una notificación específica como leída")
    @PutMapping("/{id}/leer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarComoLeida(@PathVariable Long id, Principal principal) {
        notificacionUsuarioServicio.marcarComoLeida(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Marcar todas las notificaciones del usuario como leídas")
    @PutMapping("/leer-todas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarTodasComoLeidas(Principal principal) {
        notificacionUsuarioServicio.marcarTodasComoLeidas(principal.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar una notificacion especifica")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> eliminarNotificacion(@PathVariable Long id, Principal principal) {
        notificacionUsuarioServicio.eliminarNotificacion(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

}
