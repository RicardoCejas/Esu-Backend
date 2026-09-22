package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.PacienteMapper;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteServicioTest {

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @Mock
    private PacienteMapper pacienteMapper;

    @InjectMocks
    private PacienteServicio pacienteServicio;

    @Test
    void testObtenerPacientePorId_NoEncontrado() {
        when(pacienteRepositorio.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoExcepcion.class, () -> {
            pacienteServicio.obtenerPacientePorId(999L);
        });
    }

    @Test
    void testBuscarPorDni_NoEncontrado() {
        when(pacienteRepositorio.findByDni("12345678")).thenReturn(Optional.empty());

        assertThrows(NoEncontradoExcepcion.class, () -> {
            pacienteServicio.buscarPorDni("12345678");
        });
    }
}
