package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.UsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.servicio.IUsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioControlador {

    @Autowired
    private IUsuarioServicio usuarioServicio;

    @PostMapping("/crear")
    public ResponseEntity<UsuarioDtoResponse> crearUsuario(@RequestBody UsuarioDtoRequest usuarioDto) {
        UsuarioDtoResponse nuevoUsuario = usuarioServicio.crearUsuario(usuarioDto);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<UsuarioDtoResponse> obtenerUsuarioPorId(@PathVariable Long id) {
        try {
            UsuarioDtoResponse usuario = usuarioServicio.obtenerUsuarioPorId(id);
            return new ResponseEntity<>(usuario, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/obtener/todos")
    public ResponseEntity<List<UsuarioDtoResponse>> listarTodos() {
        List<UsuarioDtoResponse> usuarios = usuarioServicio.listarTodos();
        return new ResponseEntity<>(usuarios, HttpStatus.OK);
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        try {
            usuarioServicio.eliminarUsuario(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Se usa 204 según la rúbrica (o 200)
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/buscar/username")
    public ResponseEntity<UsuarioDtoResponse> buscarPorUsername(@RequestParam String username) {
        try {
            UsuarioDtoResponse usuario = usuarioServicio.findByUsername(username);
            return new ResponseEntity<>(usuario, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/filtrar/estado")
    public ResponseEntity<List<UsuarioDtoResponse>> filtrarPorEstado(@RequestParam boolean estado) {
        List<UsuarioDtoResponse> usuarios = usuarioServicio.findByActivo(estado);
        return new ResponseEntity<>(usuarios, HttpStatus.OK);
    }
}
