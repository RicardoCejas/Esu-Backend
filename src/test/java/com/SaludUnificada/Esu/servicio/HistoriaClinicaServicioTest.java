package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.HistoriaClinicaDtoRequest;
import com.SaludUnificada.Esu.dto.response.HistoriaClinicaDtoResponse;
import com.SaludUnificada.Esu.entidad.HistoriaClinica;
import com.SaludUnificada.Esu.entidad.LogAccesoHc;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.mapper.HistoriaClinicaMapper;
import com.SaludUnificada.Esu.repositorio.HistoriaClinicaRepositorio;
import com.SaludUnificada.Esu.repositorio.LogAccesoHcRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.OwnershipSecurityEvaluator;
import com.SaludUnificada.Esu.seguridad.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    private ProfesionalRepositorio profesionalRepositorio;

    @Mock
    private OwnershipSecurityEvaluator ownershipSecurityEvaluator;

    @Mock
    private HistoriaClinicaMapper historiaClinicaMapper;

    @Mock
    private RequestIpUtil requestIpUtil;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private HistoriaClinicaServicio historiaClinicaServicio;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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

    @Test
    void testCrearHistoriaClinica_UsuarioNoAutenticado_LanzaAccessDenied() {
        HistoriaClinicaDtoRequest dto = new HistoriaClinicaDtoRequest("Gripe A", "Fiebre", "Paracetamol", 1L);
        when(pacienteRepositorio.existsById(1L)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> {
            historiaClinicaServicio.crearHistoriaClinica(dto);
        });
    }

    @Test
    void testCrearHistoriaClinica_ProfesionalSinPerfil_LanzaAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("medico_fake@hospital.gob.ar", "pass", List.of(new SimpleGrantedAuthority("ROLE_MEDICO")))
        );
        HistoriaClinicaDtoRequest dto = new HistoriaClinicaDtoRequest("Gripe A", "Fiebre", "Paracetamol", 1L);
        when(pacienteRepositorio.existsById(1L)).thenReturn(true);
        when(profesionalRepositorio.findByUsuarioEmail("medico_fake@hospital.gob.ar")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> {
            historiaClinicaServicio.crearHistoriaClinica(dto);
        });
    }

    @Test
    void testCrearHistoriaClinica_SinRelacionAsistencial_LanzaAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("medico@hospital.gob.ar", "pass", List.of(new SimpleGrantedAuthority("ROLE_MEDICO")))
        );
        HistoriaClinicaDtoRequest dto = new HistoriaClinicaDtoRequest("Gripe A", "Fiebre", "Paracetamol", 1L);
        when(pacienteRepositorio.existsById(1L)).thenReturn(true);

        Profesional prof = new Profesional();
        prof.setId(5L);
        prof.setEstadoActivo(true);
        when(profesionalRepositorio.findByUsuarioEmail("medico@hospital.gob.ar")).thenReturn(Optional.of(prof));
        when(ownershipSecurityEvaluator.canAccessPatient(1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> {
            historiaClinicaServicio.crearHistoriaClinica(dto);
        });
    }

    @Test
    void testCrearHistoriaClinica_ConRelacionAsistencial_Exitoso() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("medico@hospital.gob.ar", "pass", List.of(new SimpleGrantedAuthority("ROLE_MEDICO")))
        );
        HistoriaClinicaDtoRequest dto = new HistoriaClinicaDtoRequest("Gripe A", "Fiebre", "Paracetamol", 1L);
        when(pacienteRepositorio.existsById(1L)).thenReturn(true);

        Profesional prof = new Profesional();
        prof.setId(5L);
        prof.setEstadoActivo(true);
        when(profesionalRepositorio.findByUsuarioEmail("medico@hospital.gob.ar")).thenReturn(Optional.of(prof));
        when(ownershipSecurityEvaluator.canAccessPatient(1L)).thenReturn(true);

        HistoriaClinica hc = new HistoriaClinica();
        hc.setId(100L);
        when(historiaClinicaMapper.paraEntidad(dto)).thenReturn(hc);
        when(historiaClinicaRepositorio.save(hc)).thenReturn(hc);
        when(historiaClinicaMapper.paraDto(hc)).thenReturn(new HistoriaClinicaDtoResponse());

        HistoriaClinicaDtoResponse response = historiaClinicaServicio.crearHistoriaClinica(dto);
        assertNotNull(response);
        verify(historiaClinicaRepositorio).save(hc);
    }
}