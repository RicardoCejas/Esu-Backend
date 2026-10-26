package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.DispensarRecetaDtoRequest;
import com.SaludUnificada.Esu.dto.request.MedicamentoItemDto;
import com.SaludUnificada.Esu.dto.request.RecetaDigitalDtoRequest;
import com.SaludUnificada.Esu.dto.response.DispensarRecetaDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.error.RecetaVencidaException;
import com.SaludUnificada.Esu.error.RecetaYaDispensadaException;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecetaDigitalServicioTest {

    @Mock
    private RecetaDigitalRepositorio recetaRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @Mock
    private ProfesionalRepositorio profesionalRepositorio;

    @Mock
    private DocumentoPdfServicio pdfServicio;

    @Spy
    private CatalogoFarmacologicoServicio catalogoFarmacologicoServicio = new CatalogoFarmacologicoServicio();

    @InjectMocks
    private RecetaDigitalServicio recetaServicio;

    private Paciente paciente;
    private Profesional profesional;

    @BeforeEach
    void setUp() {
        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombre("Juan");
        paciente.setApellido("Pérez");
        paciente.setDni("30111222");

        profesional = new Profesional();
        profesional.setId(10L);
        profesional.setNombre("Carlos");
        profesional.setApellido("Gómez");
        profesional.setMatricula("MP-4589");
        profesional.setEstadoActivo(true);
    }

    @Test
    @DisplayName("HU-106: Emisión exitosa de prescripción con posología válida y principio activo genérico")
    void testEmitirReceta_Valida() {
        when(profesionalRepositorio.findById(10L)).thenReturn(Optional.of(profesional));
        when(pacienteRepositorio.findById(1L)).thenReturn(Optional.of(paciente));
        when(pdfServicio.calcularHashSha256(anyString())).thenReturn("hash_sha256_mock");
        when(recetaRepositorio.save(any(RecetaDigital.class))).thenAnswer(i -> {
            RecetaDigital r = i.getArgument(0);
            r.setId(100L);
            return r;
        });

        RecetaDigitalDtoRequest request = new RecetaDigitalDtoRequest();
        request.setPacienteId(1L);
        request.setProfesionalId(10L);
        request.setDiagnostico("Faringoamigdalitis aguda");
        request.setMedicamentos(List.of(
            new MedicamentoItemDto("Optamox", "Amoxicilina", "Comprimidos 500mg", "1 comprimido", "Cada 8 horas", "Oral", "7 días", 1)
        ));

        RecetaDigital resultado = recetaServicio.emitirReceta(request);

        assertNotNull(resultado);
        assertEquals("PENDIENTE", resultado.getEstadoReceta());
        assertEquals("hash_sha256_mock", resultado.getHashIntegridad());
        assertEquals(1, resultado.getMedicamentos().size());
        verify(recetaRepositorio, times(1)).save(any(RecetaDigital.class));
    }

    @Test
    @DisplayName("HU-106: Rechazar prescripción si contiene principios activos genéricos duplicados")
    void testEmitirReceta_RechazarDrogasDuplicadas() {
        when(profesionalRepositorio.findById(10L)).thenReturn(Optional.of(profesional));
        when(pacienteRepositorio.findById(1L)).thenReturn(Optional.of(paciente));

        RecetaDigitalDtoRequest request = new RecetaDigitalDtoRequest();
        request.setPacienteId(1L);
        request.setProfesionalId(10L);
        request.setDiagnostico("Dolor e inflamación");
        request.setMedicamentos(List.of(
            new MedicamentoItemDto("Ibupirac", "Ibuprofeno", "Comprimidos 400mg", "1 comp", "cada 8hs", "Oral", "5 días", 1),
            new MedicamentoItemDto("Actron", "ibuprofeno", "Cápsulas blandas 600mg", "1 cápsula", "cada 12hs", "Oral", "3 días", 1)
        ));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            recetaServicio.emitirReceta(request);
        });

        assertTrue(ex.getMessage().contains("No se permite prescribir medicamentos duplicados"));
        verify(recetaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("HU-105: Dispensación farmacéutica exitosa y transición de estado a DISPENSADA")
    void testDispensarReceta_Exito() {
        RecetaDigital receta = new RecetaDigital();
        receta.setId(50L);
        receta.setPaciente(paciente);
        receta.setProfesional(profesional);
        receta.setFechaEmision(LocalDate.now().minusDays(5));
        receta.setFechaVencimiento(LocalDate.now().plusDays(25));
        receta.setEstadoReceta("PENDIENTE");
        receta.setHashIntegridad("hash_receta_50");

        when(recetaRepositorio.findById(50L)).thenReturn(Optional.of(receta));
        when(recetaRepositorio.save(any(RecetaDigital.class))).thenAnswer(i -> i.getArgument(0));

        DispensarRecetaDtoRequest request = new DispensarRecetaDtoRequest("50", "FARM-CRUZ-01", "Dispensada");

        DispensarRecetaDtoResponse response = recetaServicio.dispensarReceta(request);

        assertNotNull(response);
        assertEquals("DISPENSADA", response.getEstadoReceta());
        assertEquals("FARM-CRUZ-01", response.getMatriculaFarmacia());
        assertNotNull(response.getFechaDispensacion());
        verify(recetaRepositorio).save(receta);
    }

    @Test
    @DisplayName("HU-105: Rechazar con RecetaYaDispensadaException (HTTP 409) si ya fue dispensada previamente")
    void testDispensarReceta_YaDispensada() {
        RecetaDigital receta = new RecetaDigital();
        receta.setId(51L);
        receta.setPaciente(paciente);
        receta.setProfesional(profesional);
        receta.setEstadoReceta("DISPENSADA");
        receta.setFechaDispensacion(LocalDateTime.now().minusHours(2));
        receta.setMatriculaFarmaciaDispensadora("FARM-ANTERIOR");
        receta.setHashIntegridad("hash_receta_51");

        when(recetaRepositorio.findById(51L)).thenReturn(Optional.of(receta));

        DispensarRecetaDtoRequest request = new DispensarRecetaDtoRequest("51", "FARM-NUEVA", "Intento duplicado");

        assertThrows(RecetaYaDispensadaException.class, () -> {
            recetaServicio.dispensarReceta(request);
        });

        verify(recetaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("HU-105: Rechazar con RecetaVencidaException (HTTP 410) si superó los 30 días corridos de vigencia")
    void testDispensarReceta_Vencida() {
        RecetaDigital receta = new RecetaDigital();
        receta.setId(52L);
        receta.setPaciente(paciente);
        receta.setProfesional(profesional);
        receta.setFechaEmision(LocalDate.now().minusDays(35));
        receta.setFechaVencimiento(LocalDate.now().minusDays(5)); // Vencida
        receta.setEstadoReceta("PENDIENTE");
        receta.setHashIntegridad("hash_receta_52");

        when(recetaRepositorio.findById(52L)).thenReturn(Optional.of(receta));

        DispensarRecetaDtoRequest request = new DispensarRecetaDtoRequest("52", "FARM-01", "Intento en receta vencida");

        assertThrows(RecetaVencidaException.class, () -> {
            recetaServicio.dispensarReceta(request);
        });

        assertEquals("VENCIDA", receta.getEstadoReceta());
        verify(recetaRepositorio).save(receta);
    }
}
