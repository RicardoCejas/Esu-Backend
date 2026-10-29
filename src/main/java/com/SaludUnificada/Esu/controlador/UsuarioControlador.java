package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.PreferenciasUsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.request.UsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.servicio.CarnetYFamiliarServicio;
import com.SaludUnificada.Esu.servicio.IUsuarioServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioControlador {

    @Autowired
    private IUsuarioServicio usuarioServicio;

    @Autowired
    private CarnetYFamiliarServicio carnetYFamiliarServicio;

    @PostMapping("/crear")
    public ResponseEntity<UsuarioDtoResponse> crearUsuario(@Valid @RequestBody UsuarioDtoRequest usuarioDto) {
        UsuarioDtoResponse nuevoUsuario = usuarioServicio.crearUsuario(usuarioDto);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<UsuarioDtoResponse> obtenerUsuarioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioServicio.obtenerUsuarioPorId(id));
    }

    @GetMapping("/obtener/todos")
    public ResponseEntity<List<UsuarioDtoResponse>> listarTodos() {
        return ResponseEntity.ok(usuarioServicio.listarTodos());
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioServicio.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar/username")
    public ResponseEntity<UsuarioDtoResponse> buscarPorUsername(@RequestParam String username) {
        return ResponseEntity.ok(usuarioServicio.findByUsername(username));
    }

    @GetMapping("/filtrar/estado")
    public ResponseEntity<List<UsuarioDtoResponse>> filtrarPorEstado(@RequestParam boolean estado) {
        return ResponseEntity.ok(usuarioServicio.findByActivo(estado));
    }

    @PatchMapping("/preferencias")
    public ResponseEntity<Void> actualizarPreferencias(
            Principal principal,
            @RequestParam(required = false) String email,
            @RequestBody PreferenciasUsuarioDtoRequest request) {
        String targetEmail = (principal != null && principal.getName() != null && !principal.getName().isBlank())
                ? principal.getName()
                : email;
        carnetYFamiliarServicio.actualizarPreferencias(targetEmail, request);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioDtoResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo) {
        return ResponseEntity.ok(usuarioServicio.cambiarEstadoActivo(id, activo));
    }
}
