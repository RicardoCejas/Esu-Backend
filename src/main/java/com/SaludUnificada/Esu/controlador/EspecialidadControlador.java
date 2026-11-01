package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.EspecialidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.EspecialidadDtoResponse;
import com.SaludUnificada.Esu.servicio.IEspecialidadServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
@Tag(name = "Especialidades Médicas", description = "Gestión del catálogo de especialidades clínicas")
public class EspecialidadControlador {

    @Autowired
    private IEspecialidadServicio especialidadServicio;

    @PostMapping({"", "/crear"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Crear nueva especialidad médica")
    public ResponseEntity<EspecialidadDtoResponse> crearEspecialidad(@Valid @RequestBody EspecialidadDtoRequest especialidadDto) {
        return new ResponseEntity<>(especialidadServicio.crearEspecialidad(especialidadDto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Actualizar especialidad médica existente")
    public ResponseEntity<EspecialidadDtoResponse> actualizarEspecialidad(
            @PathVariable Long id,
            @Valid @RequestBody EspecialidadDtoRequest especialidadDto) {
        return ResponseEntity.ok(especialidadServicio.actualizarEspecialidad(id, especialidadDto));
    }

    @GetMapping({"/{id:\\d+}", "/obtener/{id:\\d+}"})
    @Operation(summary = "Obtener especialidad por ID")
    public ResponseEntity<EspecialidadDtoResponse> obtenerEspecialidadPorId(@PathVariable Long id) {
        return ResponseEntity.ok(especialidadServicio.obtenerEspecialidadPorId(id));
    }

    @GetMapping({"", "/obtener/todos"})
    @Operation(summary = "Listar todas las especialidades")
    public ResponseEntity<List<EspecialidadDtoResponse>> listarTodas() {
        return ResponseEntity.ok(especialidadServicio.listarTodas());
    }

    @GetMapping("/conteo")
    @Operation(summary = "Contar especialidades registradas")
    public ResponseEntity<Long> contarEspecialidades() {
        return ResponseEntity.ok(especialidadServicio.contarEspecialidades());
    }

    @DeleteMapping({"/{id:\\d+}", "/eliminar/{id:\\d+}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Eliminar especialidad médica")
    public ResponseEntity<Void> eliminarEspecialidad(@PathVariable Long id) {
        especialidadServicio.eliminarEspecialidad(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar especialidades por nombre parcial")
    public ResponseEntity<List<EspecialidadDtoResponse>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(especialidadServicio.buscarPorNombre(nombre));
    }
}
