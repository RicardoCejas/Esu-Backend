package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.AlergiaDtoRequest;
import com.SaludUnificada.Esu.dto.request.MedicamentoItemDto;
import com.SaludUnificada.Esu.dto.response.AlergiaDtoResponse;
import com.SaludUnificada.Esu.entidad.AlergiaPaciente;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.repositorio.AlergiaPacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlergiaServicioTest {

    @Mock
    private AlergiaPacienteRepositorio alergiaRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @InjectMocks
    private AlergiaServicio alergiaServicio;

    private AlergiaPaciente alergiaGrave;

    @BeforeEach
    void setUp() {
        alergiaGrave = new AlergiaPaciente();
        alergiaGrave.setId(1L);
        alergiaGrave.setPacienteId(10L);
        alergiaGrave.setSustanciaAlergeno("Penicilina");
        alergiaGrave.setSeveridad("GRAVE");
        alergiaGrave.setActivo(true);
        alergiaGrave.setFechaRegistro(LocalDateTime.now());
    }

    @Test
    void registrarAlergiaExitosamente() {
        when(pacienteRepositorio.existsById(10L)).thenReturn(true);
        when(alergiaRepositorio.save(any(AlergiaPaciente.class))).thenReturn(alergiaGrave);

        AlergiaDtoRequest req = new AlergiaDtoRequest("Penicilina", "GRAVE", LocalDate.now(), "Riesgo anafiláctico");
        AlergiaDtoResponse resp = alergiaServicio.registrarAlergia(10L, req);

        assertNotNull(resp);
        assertEquals("Penicilina", resp.getSustanciaAlergeno());
        assertEquals("GRAVE", resp.getSeveridad());
    }

    @Test
    void registrarAlergiaFallaSiPacienteNoExiste() {
        when(pacienteRepositorio.existsById(99L)).thenReturn(false);

        AlergiaDtoRequest req = new AlergiaDtoRequest("Penicilina", "GRAVE", LocalDate.now(), null);
        assertThrows(NoEncontradoExcepcion.class, () -> alergiaServicio.registrarAlergia(99L, req));
    }

    @Test
    void validarContraindicacionesBloqueaPrescripcionConAlergiaGrave() {
        when(alergiaRepositorio.findByPacienteIdAndActivoTrueOrderByFechaRegistroDesc(10L))
                .thenReturn(List.of(alergiaGrave));

        MedicamentoItemDto med = new MedicamentoItemDto();
        med.setDroga("Amoxicilina (Derivado de Penicilina)");
        med.setNombreComercial("Amoxidal 500");

        assertThrows(ReglaNegocioException.class, () ->
                alergiaServicio.validarContraindicaciones(10L, List.of(med))
        );
    }

    @Test
    void validarContraindicacionesPermiteMedicamentoSinConflicto() {
        when(alergiaRepositorio.findByPacienteIdAndActivoTrueOrderByFechaRegistroDesc(10L))
                .thenReturn(List.of(alergiaGrave));

        MedicamentoItemDto med = new MedicamentoItemDto();
        med.setDroga("Paracetamol");
        med.setNombreComercial("Tafirol 1g");

        assertDoesNotThrow(() -> alergiaServicio.validarContraindicaciones(10L, List.of(med)));
    }
}
