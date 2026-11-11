package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.entidad.Permiso;
import com.SaludUnificada.Esu.repositorio.PermisoRepositorio;
import com.SaludUnificada.Esu.servicio.IUsuarioServicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermisoControladorTest {

    @Mock
    private PermisoRepositorio permisoRepositorio;

    @Mock
    private IUsuarioServicio usuarioServicio;

    @InjectMocks
    private PermisoControlador permisoControlador;

    @Test
    @DisplayName("listarCatalogo retorna todos los permisos del catalogo ordenados")
    void testListarCatalogo() {
        Permiso p1 = Permiso.builder().id(1L).codigo("TURNOS_VER").nombre("Ver turnos").modulo("TURNOS").build();
        Permiso p2 = Permiso.builder().id(2L).codigo("TURNOS_CANCELAR").nombre("Cancelar turnos").modulo("TURNOS").build();
        when(permisoRepositorio.findAllByOrderByModuloAscNombreAsc()).thenReturn(List.of(p1, p2));

        ResponseEntity<List<Permiso>> response = permisoControlador.listarCatalogo();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("TURNOS_VER", response.getBody().get(0).getCodigo());
        verify(permisoRepositorio, times(1)).findAllByOrderByModuloAscNombreAsc();
    }

    @Test
    @DisplayName("obtenerPermisosPorUsuario delega en usuarioServicio")
    void testObtenerPermisosPorUsuario() {
        when(usuarioServicio.obtenerPermisosUsuario(10L)).thenReturn(List.of("TURNOS_VER", "SOBRETURNOS_CREAR"));

        ResponseEntity<List<String>> response = permisoControlador.obtenerPermisosPorUsuario(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of("TURNOS_VER", "SOBRETURNOS_CREAR"), response.getBody());
        verify(usuarioServicio, times(1)).obtenerPermisosUsuario(10L);
    }

    @Test
    @DisplayName("actualizarPermisosUsuario actualiza la lista de permisos del usuario")
    void testActualizarPermisosUsuario() {
        List<String> nuevosPermisos = List.of("TURNOS_VER", "TURNOS_CANCELAR");
        UsuarioDtoResponse dto = new UsuarioDtoResponse();
        dto.setId(10L);
        dto.setPermisos(nuevosPermisos);

        when(usuarioServicio.actualizarPermisosUsuario(10L, nuevosPermisos)).thenReturn(dto);

        ResponseEntity<UsuarioDtoResponse> response = permisoControlador.actualizarPermisosUsuario(10L, nuevosPermisos);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(nuevosPermisos, response.getBody().getPermisos());
        verify(usuarioServicio, times(1)).actualizarPermisosUsuario(10L, nuevosPermisos);
    }
}
