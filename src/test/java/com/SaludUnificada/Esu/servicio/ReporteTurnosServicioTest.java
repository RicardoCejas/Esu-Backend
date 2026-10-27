package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ReporteTurnosServicioTest {

    @Mock
    private TurnoRepositorio turnoRepositorio;

    private ReporteTurnosServicio reporteTurnosServicio;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reporteTurnosServicio = new ReporteTurnosServicio(turnoRepositorio);
    }

    private Turno crearTurnoDummy() {
        Usuario usuarioPaciente = new Usuario();
        usuarioPaciente.setEmail("juan.perez@paciente.com");

        Paciente paciente = new Paciente();
        paciente.setDni("35123456");
        paciente.setNombre("Juan");
        paciente.setApellido("Perez");
        paciente.setUsuario(usuarioPaciente);

        Usuario usuarioProf = new Usuario();
        usuarioProf.setEmail("dr.gomez@clinica.com");

        Profesional prof = new Profesional();
        prof.setNombre("Carlos");
        prof.setApellido("Gomez");
        prof.setUsuario(usuarioProf);

        Turno turno = new Turno();
        turno.setId(101L);
        turno.setFechaHora(LocalDateTime.of(2026, 11, 10, 10, 30));
        turno.setEstado("CONFIRMADO");
        turno.setMotivoTurno("Control cardiológico anual");
        turno.setEsSobreturno(false);
        turno.setPaciente(paciente);
        turno.setProfesional(prof);
        turno.setFechaCheckIn(LocalDateTime.of(2026, 11, 10, 10, 15));
        turno.setRecepcionistaCheckIn("recepcion@clinica.com");
        turno.setMontoCopago(new BigDecimal("1800.00"));
        turno.setNumeroBono("BONO-4455");
        turno.setMetodoPago("EFECTIVO");
        return turno;
    }

    @Test
    void exportarTurnosExcelGeneraBytesDeArchivoValido() {
        Turno turno = crearTurnoDummy();
        when(turnoRepositorio.findReporteTurnosEntre(any(), any())).thenReturn(List.of(turno));

        byte[] excelBytes = reporteTurnosServicio.exportarTurnosExcel(null, null);

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
        // ZIP magic bytes for .xlsx (PK..)
        assertEquals((byte) 0x50, excelBytes[0]);
        assertEquals((byte) 0x4B, excelBytes[1]);
    }

    @Test
    void exportarTurnosCsvGeneraContenidoDelimitado() {
        Turno turno = crearTurnoDummy();
        when(turnoRepositorio.findReporteTurnosEntre(any(), any())).thenReturn(List.of(turno));

        byte[] csvBytes = reporteTurnosServicio.exportarTurnosCsv(null, null);

        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csvContent.contains("DNI_Paciente"));
        assertTrue(csvContent.contains("35123456"));
        assertTrue(csvContent.contains("Juan Perez"));
        assertTrue(csvContent.contains("BONO-4455"));
    }
}
