package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.ConsentimientoDtoRequest;
import com.SaludUnificada.Esu.dto.response.ConsentimientoDtoResponse;
import com.SaludUnificada.Esu.entidad.ConsentimientoInformado;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.repositorio.ConsentimientoInformadoRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsentimientoServicioTest {

    @Mock
    private ConsentimientoInformadoRepositorio consentimientoRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @Mock
    private ProfesionalRepositorio profesionalRepositorio;

    @InjectMocks
    private ConsentimientoServicio consentimientoServicio;

    private Paciente paciente;
    private Profesional profesional;

    @BeforeEach
    void setUp() {
        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombre("Carlos");
        paciente.setApellido("Perez");

        profesional = new Profesional();
        profesional.setId(2L);
        profesional.setNombre("Mariana");
        profesional.setApellido("Gomez");
        profesional.setMatricula("MP-4589");
    }

    @Test
    void firmarConsentimientoGeneraHashIntegridadSha256() {
        when(pacienteRepositorio.findById(1L)).thenReturn(Optional.of(paciente));
        when(profesionalRepositorio.findById(2L)).thenReturn(Optional.of(profesional));
        when(consentimientoRepositorio.save(any(ConsentimientoInformado.class))).thenAnswer(inv -> {
            ConsentimientoInformado ci = inv.getArgument(0);
            ci.setId(100L);
            return ci;
        });

        ConsentimientoDtoRequest req = new ConsentimientoDtoRequest(
                1L, 2L, "Endoscopía Digestiva", "Texto legal conforme ley 26.529...", "data:image/png;base64,iVBORw0KGgo..."
        );

        ConsentimientoDtoResponse resp = consentimientoServicio.firmarConsentimiento(req, "192.168.1.50");

        assertNotNull(resp);
        assertEquals("FIRMADO", resp.getEstado());
        assertNotNull(resp.getHashIntegridadSha256());
        assertEquals(64, resp.getHashIntegridadSha256().length());
        assertEquals("192.168.1.50", resp.getIpFirma());
        assertEquals("Mariana Gomez", resp.getProfesionalNombre());
    }

    @Test
    void inmutabilidadLanzaExcepcionEnPreUpdateYPreRemove() {
        ConsentimientoInformado ci = new ConsentimientoInformado();
        ci.setId(1L);

        assertThrows(IllegalStateException.class, ci::preUpdate);
        assertThrows(IllegalStateException.class, ci::preRemove);
    }
}
