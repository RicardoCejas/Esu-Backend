package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.EspecialidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.EspecialidadDtoResponse;
import com.SaludUnificada.Esu.servicio.IEspecialidadServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadControlador {

    @Autowired
    private IEspecialidadServicio especialidadServicio;

    @PostMapping("/crear")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<EspecialidadDtoResponse> crearEspecialidad(@Valid @RequestBody EspecialidadDtoRequest especialidadDto) {
        return new ResponseEntity<>(especialidadServicio.crearEspecialidad(especialidadDto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<EspecialidadDtoResponse> actualizarEspecialidad(
            @PathVariable Long id,
            @Valid @RequestBody EspecialidadDtoRequest especialidadDto) {
        return ResponseEntity.ok(especialidadServicio.actualizarEspecialidad(id, especialidadDto));
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<EspecialidadDtoResponse> obtenerEspecialidadPorId(@PathVariable Long id) {
        return ResponseEntity.ok(especialidadServicio.obtenerEspecialidadPorId(id));
    }

    @GetMapping({"", "/obtener/todos"})
    public ResponseEntity<List<EspecialidadDtoResponse>> listarTodas() {
        return ResponseEntity.ok(especialidadServicio.listarTodas());
    }

    @GetMapping("/conteo")
    public ResponseEntity<Long> contarEspecialidades() {
        return ResponseEntity.ok(especialidadServicio.contarEspecialidades());
    }

    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Void> eliminarEspecialidad(@PathVariable Long id) {
        especialidadServicio.eliminarEspecialidad(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<EspecialidadDtoResponse>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(especialidadServicio.buscarPorNombre(nombre));
    }
}
