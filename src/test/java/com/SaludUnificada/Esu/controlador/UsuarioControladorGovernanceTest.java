package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.UsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.request.UsuarioPerfilOperativoDtoRequest;
import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.repositorio.PermisoRepositorio;
import com.SaludUnificada.Esu.seguridad.OwnershipSecurityEvaluator;
import com.SaludUnificada.Esu.seguridad.PermisoGobernanzaPolicy;
import com.SaludUnificada.Esu.servicio.CarnetYFamiliarServicio;
import com.SaludUnificada.Esu.servicio.IUsuarioServicio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControladorGovernanceTest {

    @Mock
    private IUsuarioServicio usuarioServicio;

    @Mock
    private CarnetYFamiliarServicio carnetYFamiliarServicio;

    @Mock
    private OwnershipSecurityEvaluator securityPolicy;

    @Mock
    private PermisoRepositorio permisoRepositorio;

    @InjectMocks
    private UsuarioControlador usuarioControlador;

    @InjectMocks
    private PermisoControlador permisoControlador;

    @Test
    void permisoGobernanzaPolicy_stripsRestrictedCenterPermissions() {
        List<String> filtered = PermisoGobernanzaPolicy.filterCenterPermissions(List.of(
                "TURNOS_VER",
                "USUARIOS_ADMINISTRAR",
                "CONSULTORIOS_ADMINISTRAR",
                "PACIENTES_FUSIONAR",
                "REPORTES_VER"
        ));

        assertEquals(List.of("TURNOS_VER", "REPORTES_VER"), filtered);
    }

    @Test
    void crearUsuarioComoRecepcionista_filtraPermisosRestringidosYAsignaCentro() {
        ReflectionTestUtils.setField(usuarioControlador, "securityPolicy", securityPolicy);
        when(securityPolicy.isCurrentUserAdmin()).thenReturn(false);
        when(securityPolicy.getAuthenticatedCentroSaludIdOrThrow()).thenReturn(1L);
        when(securityPolicy.getAuthenticatedCentroSaludNombre()).thenReturn(Optional.of("Hospital Crespo"));

        UsuarioDtoRequest request = new UsuarioDtoRequest("nuevo.staff@esu.gob.ar", "Clave123!");
        request.setNombre("Marcos");
        request.setApellido("Ibarra");
        request.setDni("34567890");
        request.setRol("RECEPCIONISTA");
        request.setPermisos(List.of("TURNOS_VER", "USUARIOS_ADMINISTRAR", "CONSULTORIOS_ADMINISTRAR", "PACIENTES_FUSIONAR"));

        UsuarioDtoResponse resp = new UsuarioDtoResponse();
        resp.setId(50L);
        resp.setNombre("Marcos");
        resp.setApellido("Ibarra");
        resp.setDni("34567890");
        when(usuarioServicio.crearUsuario(any(UsuarioDtoRequest.class))).thenReturn(resp);

        ResponseEntity<UsuarioDtoResponse> result = usuarioControlador.crearUsuario(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, request.getCentroSaludId());
        assertEquals("Hospital Crespo", request.getCentroSaludNombre());
        assertEquals(List.of("TURNOS_VER"), request.getPermisos());
        verify(usuarioServicio).crearUsuario(request);
    }

    @Test
    void actualizarAsignacion_recepcionistaNoPuedeModificarPropioRol() {
        ReflectionTestUtils.setField(usuarioControlador, "securityPolicy", securityPolicy);
        when(securityPolicy.isCurrentUserAdmin()).thenReturn(false);
        when(securityPolicy.getAuthenticatedUserId()).thenReturn(Optional.of(15L));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                usuarioControlador.actualizarAsignacion(15L, Map.of("rol", "RECEPCIONISTA"))
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(usuarioServicio, never()).actualizarRolYCentro(any(), any(), any(), any());
    }

    @Test
    void actualizarPermisos_recepcionistaBloqueadoEnCuentaPropiaYFiltraPrivilegiosEnPares() {
        ReflectionTestUtils.setField(permisoControlador, "securityPolicy", securityPolicy);
        when(securityPolicy.isCurrentUserAdmin()).thenReturn(false);
        when(securityPolicy.getAuthenticatedUserId()).thenReturn(Optional.of(15L));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                permisoControlador.actualizarPermisosUsuario(15L, List.of("TURNOS_VER"))
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(usuarioServicio, never()).actualizarPermisosUsuario(any(), any());

        UsuarioDtoResponse updated = new UsuarioDtoResponse();
        updated.setId(22L);
        updated.setPermisos(List.of("TURNOS_VER", "PACIENTES_VER"));
        when(usuarioServicio.actualizarPermisosUsuario(eq(22L), eq(List.of("TURNOS_VER", "PACIENTES_VER"))))
                .thenReturn(updated);

        ResponseEntity<UsuarioDtoResponse> okResp = permisoControlador.actualizarPermisosUsuario(
                22L,
                List.of("TURNOS_VER", "PACIENTES_VER", "USUARIOS_ADMINISTRAR", "CONSULTORIOS_ADMINISTRAR", "PACIENTES_FUSIONAR")
        );

        assertEquals(HttpStatus.OK, okResp.getStatusCode());
        verify(usuarioServicio).actualizarPermisosUsuario(22L, List.of("TURNOS_VER", "PACIENTES_VER"));
    }

    @Test
    void revocarSesionesYPerfilOperativo_permitenRecepcionDeSede() throws NoSuchMethodException {
        Method revokeMethod = UsuarioControlador.class.getMethod("revocarSesionesUsuario", Long.class);
        PreAuthorize revokeAuth = revokeMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(revokeAuth);
        assertTrue(revokeAuth.value().contains("isReceptionistOfUser(#id)"));

        Method profileMethod = UsuarioControlador.class.getMethod("actualizarPerfilOperativo", Long.class, UsuarioPerfilOperativoDtoRequest.class);
        PreAuthorize profileAuth = profileMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(profileAuth);
        assertTrue(profileAuth.value().contains("isReceptionistOfUser(#id)"));

        ResponseEntity<Void> revokeResp = usuarioControlador.revocarSesionesUsuario(33L);
        assertEquals(HttpStatus.NO_CONTENT, revokeResp.getStatusCode());
        verify(usuarioServicio).revocarSesiones(33L);

        UsuarioPerfilOperativoDtoRequest perfilReq = new UsuarioPerfilOperativoDtoRequest("Laura", "Gomez", "30111222");
        UsuarioDtoResponse perfilDto = new UsuarioDtoResponse();
        perfilDto.setId(33L);
        perfilDto.setNombre("Laura");
        when(usuarioServicio.actualizarPerfilOperativo(33L, perfilReq)).thenReturn(perfilDto);

        ResponseEntity<UsuarioDtoResponse> perfilResp = usuarioControlador.actualizarPerfilOperativo(33L, perfilReq);
        assertEquals(HttpStatus.OK, perfilResp.getStatusCode());
        assertEquals("Laura", perfilResp.getBody().getNombre());
    }
}
