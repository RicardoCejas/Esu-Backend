package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.ConsultaBorradorDtoRequest;
import com.SaludUnificada.Esu.dto.request.EvolucionMedicaDtoRequest;
import com.SaludUnificada.Esu.dto.response.ConsultaBorradorDtoResponse;
import com.SaludUnificada.Esu.entidad.ConsultaBorrador;
import com.SaludUnificada.Esu.entidad.EvolucionMedica;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.repositorio.ConsultaBorradorRepositorio;
import com.SaludUnificada.Esu.repositorio.EvolucionMedicaRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import com.SaludUnificada.Esu.seguridad.OwnershipSecurityEvaluator;
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
class EvolucionMedicaServicioTest {

    @Mock
    private EvolucionMedicaRepositorio evolucionRepositorio;

    @Mock
    private ConsultaBorradorRepositorio borradorRepositorio;

    @Mock
    private ProfesionalRepositorio profesionalRepositorio;

    @Mock
    private TurnoRepositorio turnoRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @Mock
    private OwnershipSecurityEvaluator ownershipSecurityEvaluator;

    @Mock
    private DocumentoPdfServicio pdfServicio;

    @Mock
    private RecetaDigitalServicio recetaServicio;

    @InjectMocks
    private EvolucionMedicaServicio evolucionMedicaServicio;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGuardarBorrador_SinEmailAutenticado_FallaAccesoDenegado() {
        ConsultaBorradorDtoRequest req = new ConsultaBorradorDtoRequest();
        req.setPacienteId(10L);

        assertThrows(AccessDeniedException.class, () -> {
            evolucionMedicaServicio.guardarBorradorConsulta(req, null);
        });
    }

    @Test
    void testGuardarBorrador_TurnoAsignadoAOtroProfesional_FallaAccesoDenegado() {
        ConsultaBorradorDtoRequest req = new ConsultaBorradorDtoRequest();
        req.setTurnoId(100L);
        req.setPacienteId(10L);

        Profesional profesionalAutenticado = new Profesional();
        profesionalAutenticado.setId(1L);
        when(profesionalRepositorio.findByUsuarioEmail("medico1@esu.com"))
                .thenReturn(Optional.of(profesionalAutenticado));

        Profesional otroProfesional = new Profesional();
        otroProfesional.setId(2L);
        Turno turno = new Turno();
        turno.setId(100L);
        turno.setProfesional(otroProfesional);

        when(turnoRepositorio.findById(100L)).thenReturn(Optional.of(turno));

        assertThrows(AccessDeniedException.class, () -> {
            evolucionMedicaServicio.guardarBorradorConsulta(req, "medico1@esu.com");
        });
    }

    @Test
    void testGuardarBorrador_Exitoso() {
        ConsultaBorradorDtoRequest req = new ConsultaBorradorDtoRequest();
        req.setTurnoId(100L);
        req.setPacienteId(10L);
        req.setMotivoConsulta("Dolor de cabeza");
        req.setDiagnostico("Cefalea tensional");

        Profesional profesionalAutenticado = new Profesional();
        profesionalAutenticado.setId(1L);
        when(profesionalRepositorio.findByUsuarioEmail("medico1@esu.com"))
                .thenReturn(Optional.of(profesionalAutenticado));

        Paciente paciente = new Paciente();
        paciente.setId(10L);
        Turno turno = new Turno();
        turno.setId(100L);
        turno.setProfesional(profesionalAutenticado);
        turno.setPaciente(paciente);

        when(turnoRepositorio.findById(100L)).thenReturn(Optional.of(turno));
        when(pacienteRepositorio.existsById(10L)).thenReturn(true);
        when(borradorRepositorio.findByTurnoId(100L)).thenReturn(Optional.empty());

        ConsultaBorradorDtoResponse resp = evolucionMedicaServicio.guardarBorradorConsulta(req, "medico1@esu.com");

        assertTrue(resp.isGuardado());
        verify(borradorRepositorio).save(any(ConsultaBorrador.class));
    }

    @Test
    void testObtenerBorrador_TurnoAsignadoAOtroProfesional_FallaAccesoDenegado() {
        Profesional profesionalAutenticado = new Profesional();
        profesionalAutenticado.setId(1L);
        when(profesionalRepositorio.findByUsuarioEmail("medico1@esu.com"))
                .thenReturn(Optional.of(profesionalAutenticado));

        Profesional otro = new Profesional();
        otro.setId(2L);
        Turno turno = new Turno();
        turno.setId(100L);
        turno.setProfesional(otro);

        when(turnoRepositorio.findById(100L)).thenReturn(Optional.of(turno));

        assertThrows(AccessDeniedException.class, () -> {
            evolucionMedicaServicio.obtenerBorradorConsulta(100L, null, "medico1@esu.com");
        });
    }

    @Test
    void testRegistrarEvolucion_ManipulacionProfesionalId_LanzaAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("medico1@esu.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_MEDICO")))
        );

        EvolucionMedicaDtoRequest req = new EvolucionMedicaDtoRequest();
        req.setPacienteId(10L);
        req.setProfesionalId(2L); // ID manipulado en el body

        Paciente pac = new Paciente();
        pac.setId(10L);
        when(pacienteRepositorio.findById(10L)).thenReturn(Optional.of(pac));

        Profesional prof2 = new Profesional();
        prof2.setId(2L);
        Usuario u2 = new Usuario();
        u2.setEmail("otro_medico@esu.com");
        prof2.setUsuario(u2);
        when(profesionalRepositorio.findById(2L)).thenReturn(Optional.of(prof2));

        Profesional prof1 = new Profesional();
        prof1.setId(1L);
        prof1.setEstadoActivo(true);
        when(profesionalRepositorio.findByUsuarioEmail("medico1@esu.com")).thenReturn(Optional.of(prof1));

        assertThrows(AccessDeniedException.class, () -> {
            evolucionMedicaServicio.registrarEvolucion(req);
        });
    }

    @Test
    void testRegistrarEvolucion_TurnoInconsistenteConPaciente_LanzaReglaNegocio() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("medico1@esu.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_MEDICO")))
        );

        EvolucionMedicaDtoRequest req = new EvolucionMedicaDtoRequest();
        req.setPacienteId(10L);
        req.setProfesionalId(1L);
        req.setTurnoId(50L);

        Paciente pac = new Paciente();
        pac.setId(10L);
        when(pacienteRepositorio.findById(10L)).thenReturn(Optional.of(pac));

        Profesional prof1 = new Profesional();
        prof1.setId(1L);
        prof1.setEstadoActivo(true);
        Usuario u1 = new Usuario();
        u1.setEmail("medico1@esu.com");
        prof1.setUsuario(u1);
        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(prof1));
        when(profesionalRepositorio.findByUsuarioEmail("medico1@esu.com")).thenReturn(Optional.of(prof1));

        Turno turno = new Turno();
        turno.setId(50L);
        Paciente otroPaciente = new Paciente();
        otroPaciente.setId(99L); // Distinto paciente
        turno.setPaciente(otroPaciente);
        turno.setProfesional(prof1);
        when(turnoRepositorio.findById(50L)).thenReturn(Optional.of(turno));

        assertThrows(ReglaNegocioException.class, () -> {
            evolucionMedicaServicio.registrarEvolucion(req);
        });
    }

    @Test
    void testRegistrarEvolucion_SinTurnoYSinRelacionAsistencial_LanzaAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("medico1@esu.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_MEDICO")))
        );

        EvolucionMedicaDtoRequest req = new EvolucionMedicaDtoRequest();
        req.setPacienteId(10L);
        req.setProfesionalId(1L);
        req.setTurnoId(null); // Sin turno previo

        Paciente pac = new Paciente();
        pac.setId(10L);
        when(pacienteRepositorio.findById(10L)).thenReturn(Optional.of(pac));

        Profesional prof1 = new Profesional();
        prof1.setId(1L);
        prof1.setEstadoActivo(true);
        Usuario u1 = new Usuario();
        u1.setEmail("medico1@esu.com");
        prof1.setUsuario(u1);
        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(prof1));
        when(profesionalRepositorio.findByUsuarioEmail("medico1@esu.com")).thenReturn(Optional.of(prof1));

        when(ownershipSecurityEvaluator.canAccessPatient(10L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> {
            evolucionMedicaServicio.registrarEvolucion(req);
        });
    }

    @Test
    void testRegistrarEvolucion_SinTurnoConRelacionAsistencial_Exitoso() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("medico1@esu.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_MEDICO")))
        );

        EvolucionMedicaDtoRequest req = new EvolucionMedicaDtoRequest();
        req.setPacienteId(10L);
        req.setProfesionalId(1L);
        req.setTurnoId(null); // Sin turno previo
        req.setDiagnostico("Rinitis alérgica");
        req.setCodigoCie10("J30");

        Paciente pac = new Paciente();
        pac.setId(10L);
        when(pacienteRepositorio.findById(10L)).thenReturn(Optional.of(pac));

        Profesional prof1 = new Profesional();
        prof1.setId(1L);
        prof1.setEstadoActivo(true);
        Usuario u1 = new Usuario();
        u1.setEmail("medico1@esu.com");
        prof1.setUsuario(u1);
        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(prof1));
        when(profesionalRepositorio.findByUsuarioEmail("medico1@esu.com")).thenReturn(Optional.of(prof1));

        when(ownershipSecurityEvaluator.canAccessPatient(10L)).thenReturn(true);
        when(evolucionRepositorio.save(any(EvolucionMedica.class))).thenAnswer(inv -> inv.getArgument(0));

        EvolucionMedica resultado = evolucionMedicaServicio.registrarEvolucion(req);

        assertNotNull(resultado);
        assertEquals("Rinitis alérgica", resultado.getDiagnostico());
        verify(evolucionRepositorio).save(any(EvolucionMedica.class));
    }
}