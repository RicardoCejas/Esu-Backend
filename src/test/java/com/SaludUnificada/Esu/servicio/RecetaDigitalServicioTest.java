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
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import com.SaludUnificada.Esu.dto.response.RecetaValidacionDtoResponse;
import com.SaludUnificada.Esu.dto.response.VerificacionRecetaPublicaDtoResponse;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
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
    private TurnoRepositorio turnoRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @Mock
    private ProfesionalRepositorio profesionalRepositorio;

    @Mock
    private DocumentoPdfServicio pdfServicio;

    @Mock
    private AlergiaServicio alergiaServicio;

    @Mock
    private EmailNotificacionServicio emailServicio;

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
    @Test
    @DisplayName("Scheduler: Marcar recetas vencidas en lote")
    void testMarcarRecetasVencidasBatch() {
        RecetaDigital r1 = new RecetaDigital();
        r1.setId(101L);
        r1.setEstadoReceta("EMITIDA");
        r1.setFechaVencimiento(LocalDate.now().minusDays(2));

        RecetaDigital r2 = new RecetaDigital();
        r2.setId(102L);
        r2.setEstadoReceta("EMITIDA");
        r2.setFechaVencimiento(LocalDate.now().minusDays(1));

        when(recetaRepositorio.findByEstadoRecetaAndFechaVencimientoLessThan(eq("EMITIDA"), any(LocalDate.class)))
                .thenReturn(List.of(r1, r2));

        int marcadas = recetaServicio.marcarRecetasVencidasBatch(50);

        assertEquals(2, marcadas);
        assertEquals("VENCIDA", r1.getEstadoReceta());
        assertEquals("VENCIDA", r2.getEstadoReceta());
        verify(recetaRepositorio, times(2)).save(any(RecetaDigital.class));
    }

    @Test
    @DisplayName("Fase 1: Validación exitosa para dispensación usando hash de alta entropía")
    void testValidarRecetaParaDispensacion_Exito() {
        String hash = "a1b2c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef";
        RecetaDigital r = new RecetaDigital();
        r.setId(99L);
        r.setHashIntegridad(hash);
        r.setEstadoReceta("EMITIDA");
        r.setPaciente(paciente);
        r.setProfesional(profesional);
        r.setMedicamentos(List.of());

        when(recetaRepositorio.findByHashIntegridad(hash)).thenReturn(Optional.of(r));

        RecetaValidacionDtoResponse resp = recetaServicio.validarRecetaParaDispensacion(hash);

        assertNotNull(resp);
        assertEquals(hash, resp.getCodigoReceta());
        assertEquals("EMITIDA", resp.getEstado());
        assertEquals("30111222", resp.getPacienteDni());
    }

    @Test
    @DisplayName("Fase 1: Rechazo de consulta por ID numérico en validación de dispensación")
    void testValidarRecetaParaDispensacion_RechazarIdNumerico() {
        assertThrows(NoEncontradoExcepcion.class, () -> {
            recetaServicio.validarRecetaParaDispensacion("12345");
        });
        verify(recetaRepositorio, never()).findByHashIntegridad(anyString());
    }

    @Test
    @DisplayName("Fase 1: Verificación pública con hash válido oculta ID numérico primario")
    void testVerificarDocumentoPublico_OcultaIdNumerico() {
        String hash = "c1c2c3c4c5c6c7c8c9c0d1d2d3d4d5d6d7d8d9d0e1e2e3e4e5e6e7e8e9e0f1f2";
        RecetaDigital r = new RecetaDigital();
        r.setId(77L);
        r.setHashIntegridad(hash);
        r.setEstadoReceta("VIGENTE");
        r.setPaciente(paciente);
        r.setProfesional(profesional);
        r.setMedicamentos(List.of());

        when(recetaRepositorio.findByHashIntegridad(hash)).thenReturn(Optional.of(r));
        when(pdfServicio.calcularHashSha256(anyString())).thenReturn(hash);

        VerificacionRecetaPublicaDtoResponse resp = recetaServicio.verificarDocumentoPublico(hash);

        assertTrue(resp.isValido());
        assertNull(resp.getId(), "El ID numérico interno nunca debe exponerse en el endpoint público");
        assertEquals(hash, resp.getHashIntegridadSha256());
    }

    @Test
    @DisplayName("Fase 1: Verificación pública rechaza código numérico de baja entropía")
    void testVerificarDocumentoPublico_RechazaIdNumerico() {
        VerificacionRecetaPublicaDtoResponse resp = recetaServicio.verificarDocumentoPublico("77");

        assertFalse(resp.isValido());
        assertFalse(resp.isIntegridadValida());
        verify(recetaRepositorio, never()).findByHashIntegridad(anyString());
    }

    @Test
    @DisplayName("Fase 1: Filtrar recetas por CIE-10 impidiendo acceso sin relación asistencial")
    void testListarPorCodigoCie10ParaProfesional_FiltraSinRelacion() {
        String cie10 = "J00";
        Profesional profAuth = new Profesional();
        profAuth.setId(10L);

        Paciente pacConRelacion = new Paciente();
        pacConRelacion.setId(1L);

        Paciente pacSinRelacion = new Paciente();
        pacSinRelacion.setId(2L);

        RecetaDigital r1 = new RecetaDigital();
        r1.setId(101L);
        r1.setProfesional(profAuth);
        r1.setPaciente(pacConRelacion);
        r1.setCodigoCie10(cie10);

        RecetaDigital r2 = new RecetaDigital();
        r2.setId(102L);
        Profesional otroProf = new Profesional();
        otroProf.setId(99L);
        r2.setProfesional(otroProf);
        r2.setPaciente(pacSinRelacion);
        r2.setCodigoCie10(cie10);

        when(profesionalRepositorio.findByUsuarioEmail("medico@esu.com")).thenReturn(Optional.of(profAuth));
        when(recetaRepositorio.findByCodigoCie10(cie10)).thenReturn(List.of(r1, r2));
        when(turnoRepositorio.existsByProfesionalIdAndPacienteId(10L, 2L)).thenReturn(false);
        when(recetaRepositorio.existsByProfesionalIdAndPacienteId(10L, 2L)).thenReturn(false);

        org.springframework.test.util.ReflectionTestUtils.setField(recetaServicio, "turnoRepositorio", turnoRepositorio);
        List<RecetaDigital> resultado = recetaServicio.listarPorCodigoCie10ParaProfesional(cie10, "medico@esu.com");

        assertEquals(1, resultado.size());
        assertEquals(101L, resultado.get(0).getId());
    }
}

