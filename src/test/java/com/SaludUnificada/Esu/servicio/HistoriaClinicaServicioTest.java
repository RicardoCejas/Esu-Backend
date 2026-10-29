package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.LogAccesoHc;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.repositorio.HistoriaClinicaRepositorio;
import com.SaludUnificada.Esu.repositorio.LogAccesoHcRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoriaClinicaServicioTest {

    @Mock
    private HistoriaClinicaRepositorio historiaClinicaRepositorio;

    @Mock
    private LogAccesoHcRepositorio logAccesoHcRepositorio;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @Mock
    private RequestIpUtil requestIpUtil;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private HistoriaClinicaServicio historiaClinicaServicio;

    @Test
    void testEliminarHistoriaClinica_RechazaBorradoFisicoPorLey26529() {
        when(historiaClinicaRepositorio.existsById(10L)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> {
            historiaClinicaServicio.eliminarHistoriaClinica(10L);
        });

        assertTrue(ex.getMessage().contains("Ley 26.529"));
        verify(historiaClinicaRepositorio, never()).deleteById(anyLong());
    }

    @Test
    void testRegistrarAccesoEmergencia_MotivoInvalido_LanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> {
            historiaClinicaServicio.registrarAccesoEmergencia(1L, "medico@clinica.com", "Corto", httpServletRequest);
        });
    }

    @Test
    void testRegistrarAccesoEmergencia_RolNoFacultativo_LanzaAccessDenied() {
        when(pacienteRepositorio.existsById(1L)).thenReturn(true);
        Usuario usuarioRecepcion = new Usuario();
        usuarioRecepcion.setId(20L);
        usuarioRecepcion.setRol("RECEPCIONISTA");
        when(usuarioRepositorio.findByEmail("recepcion@clinica.com")).thenReturn(Optional.of(usuarioRecepcion));

        assertThrows(AccessDeniedException.class, () -> {
            historiaClinicaServicio.registrarAccesoEmergencia(1L, "recepcion@clinica.com", "Urgencia médica en guardia", httpServletRequest);
        });
    }

    @Test
    void testRegistrarAccesoEmergencia_Exitoso() {
        when(pacienteRepositorio.existsById(1L)).thenReturn(true);
        Usuario usuarioMedico = new Usuario();
        usuarioMedico.setId(30L);
        usuarioMedico.setRol("MEDICO");
        when(usuarioRepositorio.findByEmail("medico@clinica.com")).thenReturn(Optional.of(usuarioMedico));
        when(requestIpUtil.obtenerIpCliente(httpServletRequest)).thenReturn("192.168.1.50");

        historiaClinicaServicio.registrarAccesoEmergencia(1L, "medico@clinica.com", "Ingreso en coma al shock room", httpServletRequest);

        verify(logAccesoHcRepositorio).save(any(LogAccesoHc.class));
    }
}
