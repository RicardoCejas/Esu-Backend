package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.EspecialidadDtoRequest;
import com.SaludUnificada.Esu.dto.response.EspecialidadDtoResponse;
import com.SaludUnificada.Esu.servicio.IEspecialidadServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadControlador {

    @Autowired
    private IEspecialidadServicio especialidadServicio;

    @PostMapping("/crear")
    public ResponseEntity<EspecialidadDtoResponse> crearEspecialidad(@RequestBody EspecialidadDtoRequest especialidadDto) {
        EspecialidadDtoResponse nuevaEspecialidad = especialidadServicio.crearEspecialidad(especialidadDto);
        return new ResponseEntity<>(nuevaEspecialidad, HttpStatus.CREATED);
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<EspecialidadDtoResponse> obtenerEspecialidadPorId(@PathVariable Long id) {
        try {
            EspecialidadDtoResponse especialidad = especialidadServicio.obtenerEspecialidadPorId(id);
            return new ResponseEntity<>(especialidad, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/obtener/todos")
    public ResponseEntity<List<EspecialidadDtoResponse>> listarTodas() {
        List<EspecialidadDtoResponse> especialidades = especialidadServicio.listarTodas();
        return new ResponseEntity<>(especialidades, HttpStatus.OK);
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarEspecialidad(@PathVariable Long id) {
        try {
            especialidadServicio.eliminarEspecialidad(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<EspecialidadDtoResponse>> buscarPorNombre(@RequestParam String nombre) {
        List<EspecialidadDtoResponse> especialidades = especialidadServicio.buscarPorNombre(nombre);
        return new ResponseEntity<>(especialidades, HttpStatus.OK);
    }
}
