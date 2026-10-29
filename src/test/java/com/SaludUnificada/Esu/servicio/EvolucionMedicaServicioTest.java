package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.ConsultaBorradorDtoRequest;
import com.SaludUnificada.Esu.dto.response.ConsultaBorradorDtoResponse;
import com.SaludUnificada.Esu.entidad.ConsultaBorrador;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.repositorio.ConsultaBorradorRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvolucionMedicaServicioTest {

    @Mock
    private ConsultaBorradorRepositorio borradorRepositorio;

    @Mock
    private ProfesionalRepositorio profesionalRepositorio;

    @Mock
    private TurnoRepositorio turnoRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @InjectMocks
    private EvolucionMedicaServicio evolucionMedicaServicio;

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
}
