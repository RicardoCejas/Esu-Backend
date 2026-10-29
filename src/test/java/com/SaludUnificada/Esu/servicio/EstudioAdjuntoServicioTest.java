package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.EstudioAdjuntoDtoResponse;
import com.SaludUnificada.Esu.entidad.EstudioAdjunto;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.repositorio.EstudioAdjuntoRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstudioAdjuntoServicioTest {

    @Mock
    private EstudioAdjuntoRepositorio estudioRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @InjectMocks
    private EstudioAdjuntoServicio estudioServicio;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(estudioServicio, "directorioCargas", tempDir.toString());
    }

    @Test
    void almacenarEstudioPdfExitosoConSha256() {
        when(pacienteRepositorio.existsById(5L)).thenReturn(true);
        when(estudioRepositorio.save(any(EstudioAdjunto.class))).thenAnswer(inv -> {
            EstudioAdjunto e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        MockMultipartFile file = new MockMultipartFile(
                "archivo", "laboratorio_sangre.pdf", "application/pdf", "%PDF-1.4 sample content".getBytes()
        );

        EstudioAdjuntoDtoResponse resp = estudioServicio.almacenarEstudio(file, 5L, null, "LABORATORIO", LocalDate.now());

        assertNotNull(resp);
        assertEquals("laboratorio_sangre.pdf", resp.getNombreArchivo());
        assertEquals("LABORATORIO", resp.getTipoEstudio());
        assertNotNull(resp.getChecksumSha256());
        assertEquals(64, resp.getChecksumSha256().length());
    }

    @Test
    void rechazaArchivoEjecutablePorSeguridad() {
        when(pacienteRepositorio.existsById(5L)).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile(
                "archivo", "malware.exe", "application/octet-stream", "MZ dummy".getBytes()
        );

        assertThrows(ReglaNegocioException.class, () ->
                estudioServicio.almacenarEstudio(file, 5L, null, "INFORME", LocalDate.now())
        );
    }
    @Test
    void testDescargarArchivo_BloqueaPathTraversal() {
        EstudioAdjunto estudio = new EstudioAdjunto();
        estudio.setId(99L);
        estudio.setRutaAlmacenamiento(tempDir.getParent().resolve("archivo_secreto.txt").toString());

        when(estudioRepositorio.findById(99L)).thenReturn(java.util.Optional.of(estudio));

        assertThrows(SecurityException.class, () ->
                estudioServicio.descargarArchivo(99L)
        );
    }
}
