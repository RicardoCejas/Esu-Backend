package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.SobreturnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.mapper.TurnoMapper;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SobreturnoServicioTest {

    @Mock
    private TurnoRepositorio turnoRepositorio;

    @Mock
    private ProfesionalRepositorio profesionalRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @Mock
    private TurnoMapper turnoMapper;

    @InjectMocks
    private TurnoServicio turnoServicio;

    private Profesional profesional;
    private Paciente paciente;
    private SobreturnoDtoRequest request;

    @BeforeEach
    void setUp() {
        profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Carlos");
        profesional.setApellido("Martínez");

        paciente = new Paciente();
        paciente.setId(10L);
        paciente.setNombre("Juan");
        paciente.setApellido("Pérez");
        paciente.setDni("30111222");

        request = SobreturnoDtoRequest.builder()
                .profesionalId(1L)
                .pacienteId(10L)
                .fechaHora(LocalDateTime.now().plusDays(1).withHour(11).withMinute(30))
                .motivoSobreturno("Urgencia menor por crisis hipertensiva leve")
                .build();
    }

    @Test
    @DisplayName("Debe crear sobreturno exitosamente cuando no se ha alcanzado el límite diario")
    void testCrearSobreturno_Exitoso() {
        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(profesional));
        when(pacienteRepositorio.findById(10L)).thenReturn(Optional.of(paciente));
        // Supongamos que ya tiene 1 sobreturno hoy (el límite es 3)
        when(turnoRepositorio.contarSobreturnosEnDia(eq(1L), any(), any())).thenReturn(1L);
        when(turnoRepositorio.save(any(Turno.class))).thenAnswer(invocation -> {
            Turno t = invocation.getArgument(0);
            t.setId(100L);
            return t;
        });

        TurnoDtoResponse esperado = TurnoDtoResponse.builder()
                .id(100L)
                .fechaHora(request.getFechaHora())
                .estado("PENDIENTE")
                .esSobreturno(true)
                .motivoSobreturno("Urgencia menor por crisis hipertensiva leve")
                .build();

        when(turnoMapper.paraDto(any(Turno.class))).thenReturn(esperado);

        TurnoDtoResponse response = turnoServicio.crearSobreturno(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertTrue(response.getEsSobreturno());
        assertEquals("Urgencia menor por crisis hipertensiva leve", response.getMotivoSobreturno());
        assertEquals("PENDIENTE", response.getEstado());

        verify(turnoRepositorio).save(argThat(Turno::getEsSobreturno));
    }

    @Test
    @DisplayName("Debe lanzar ReglaNegocioException cuando se alcanza el cupo máximo diario de sobreturnos")
    void testCrearSobreturno_SuperaLimite_LanzaReglaNegocioException() {
        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(profesional));
        when(pacienteRepositorio.findById(10L)).thenReturn(Optional.of(paciente));
        // Ya tiene 3 sobreturnos hoy (límite máximo alcanzado)
        when(turnoRepositorio.contarSobreturnosEnDia(eq(1L), any(), any())).thenReturn(3L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                turnoServicio.crearSobreturno(request));

        assertTrue(ex.getMessage().contains("cupo máximo de sobreturnos"));
        verify(turnoRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar NoEncontradoExcepcion cuando el profesional no existe")
    void testCrearSobreturno_ProfesionalNoEncontrado() {
        when(profesionalRepositorio.findById(99L)).thenReturn(Optional.empty());
        request.setProfesionalId(99L);

        assertThrows(NoEncontradoExcepcion.class, () ->
                turnoServicio.crearSobreturno(request));
    }

    @Test
    @DisplayName("Debe lanzar NoEncontradoExcepcion cuando el paciente no existe")
    void testCrearSobreturno_PacienteNoEncontrado() {
        when(profesionalRepositorio.findById(1L)).thenReturn(Optional.of(profesional));
        when(pacienteRepositorio.findById(99L)).thenReturn(Optional.empty());
        request.setPacienteId(99L);

        assertThrows(NoEncontradoExcepcion.class, () ->
                turnoServicio.crearSobreturno(request));
    }
}
