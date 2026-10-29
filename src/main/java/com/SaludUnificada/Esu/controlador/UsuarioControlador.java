package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.PreferenciasUsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.request.UsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.servicio.CarnetYFamiliarServicio;
import com.SaludUnificada.Esu.servicio.IUsuarioServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioControlador {

    private final IUsuarioServicio usuarioServicio;
    private final CarnetYFamiliarServicio carnetYFamiliarServicio;

    @PostMapping("/crear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioDtoResponse> crearUsuario(@Valid @RequestBody UsuarioDtoRequest usuarioDto) {
        UsuarioDtoResponse nuevoUsuario = usuarioServicio.crearUsuario(usuarioDto);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioDtoResponse> obtenerUsuarioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioServicio.obtenerUsuarioPorId(id));
    }

    @GetMapping("/obtener/todos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioDtoResponse>> listarTodos() {
        return ResponseEntity.ok(usuarioServicio.listarTodos());
    }

    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioDtoResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo) {
        return ResponseEntity.ok(usuarioServicio.cambiarEstadoActivo(id, activo));
    }
}
