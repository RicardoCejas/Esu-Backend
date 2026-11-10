package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.UsuarioDtoRequest;
import com.SaludUnificada.Esu.dto.response.UsuarioDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.mapper.UsuarioMapper;
import com.SaludUnificada.Esu.repositorio.EvolucionMedicaRepositorio;
import com.SaludUnificada.Esu.repositorio.HistoriaClinicaRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServicioTest {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @Mock
    private HistoriaClinicaRepositorio historiaClinicaRepositorio;

    @Mock
    private EvolucionMedicaRepositorio evolucionMedicaRepositorio;

    @Mock
    private RecetaDigitalRepositorio recetaDigitalRepositorio;

    @InjectMocks
    private UsuarioServicio usuarioServicio;

    @Test
    void testCrearUsuario_EmailDuplicado_LanzaExcepcion() {
        UsuarioDtoRequest req = new UsuarioDtoRequest();
        req.setUsername("duplicado@esu.salud.gob.ar");

        when(usuarioRepositorio.existsByEmail("duplicado@esu.salud.gob.ar")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            usuarioServicio.crearUsuario(req);
        });
    }

    @Test
    void testCrearUsuario_Exitoso() {
        UsuarioDtoRequest req = new UsuarioDtoRequest();
        req.setUsername("nuevo@esu.salud.gob.ar");
        req.setPassword("Password123!");

        Usuario entidad = new Usuario();
        entidad.setEmail("nuevo@esu.salud.gob.ar");
        entidad.setPassword("Password123!");

        when(usuarioRepositorio.existsByEmail("nuevo@esu.salud.gob.ar")).thenReturn(false);
        when(usuarioMapper.paraEntidad(req)).thenReturn(entidad);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded_pass");
        when(usuarioRepositorio.save(any(Usuario.class))).thenReturn(entidad);
        when(usuarioMapper.paraDto(entidad)).thenReturn(new UsuarioDtoResponse());

        UsuarioDtoResponse resp = usuarioServicio.crearUsuario(req);
        assertNotNull(resp);
        assertEquals("encoded_pass", entidad.getPassword());
        verify(usuarioRepositorio).save(entidad);
    }

    @Test
    void testEliminarUsuario_NoExiste_LanzaNoEncontrado() {
        when(usuarioRepositorio.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoExcepcion.class, () -> {
            usuarioServicio.eliminarUsuario(999L);
        });
    }

    @Test
    void testEliminarUsuario_SinDatosClinicos_EliminaFisicamente() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        when(usuarioRepositorio.findById(10L)).thenReturn(Optional.of(usuario));
        when(pacienteRepositorio.findByUsuarioId(10L)).thenReturn(Optional.empty());

        usuarioServicio.eliminarUsuario(10L);

        verify(usuarioRepositorio).delete(usuario);
    }

    @Test
    void testEliminarUsuario_ConDatosClinicosDePaciente_BloqueaBorradoFisicoYAplicaBajaLogica() {
        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setEstadoActivo(true);
        usuario.setTokenVersion(1);

        Paciente paciente = new Paciente();
        paciente.setId(50L);

        when(usuarioRepositorio.findById(20L)).thenReturn(Optional.of(usuario));
        when(pacienteRepositorio.findByUsuarioId(20L)).thenReturn(Optional.of(paciente));
        when(historiaClinicaRepositorio.countByPacienteId(50L)).thenReturn(1L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> {
            usuarioServicio.eliminarUsuario(20L);
        });

        assertTrue(ex.getMessage().contains("Ley 26.529"));
        assertFalse(usuario.getEstadoActivo());
        assertEquals(2, usuario.getTokenVersion());
        verify(usuarioRepositorio).save(usuario);
        verify(usuarioRepositorio, never()).delete(any());
        verify(usuarioRepositorio, never()).deleteById(anyLong());
    }
    @Test
    void testActualizarRolYCentro_Exitoso_RevocaSesionesYGuarda() {
        Usuario usuario = new Usuario();
        usuario.setId(30L);
        usuario.setRol("ROLE_RECEPCIONISTA");
        usuario.setTokenVersion(1);

        when(usuarioRepositorio.findById(30L)).thenReturn(Optional.of(usuario));
        when(usuarioRepositorio.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.paraDto(usuario)).thenReturn(new UsuarioDtoResponse());

        UsuarioDtoResponse resp = usuarioServicio.actualizarRolYCentro(30L, "ROLE_ADMIN", 5L, "Hospital Central");

        assertNotNull(resp);
        assertEquals("ROLE_ADMIN", usuario.getRol());
        assertEquals(5L, usuario.getCentroSaludId());
        assertEquals("Hospital Central", usuario.getCentroSaludNombre());
        assertEquals(2, usuario.getTokenVersion());
        verify(usuarioRepositorio).save(usuario);
    }
}
