package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.PacienteMapper;
import com.SaludUnificada.Esu.repositorio.*;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteServicioTest {

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @Mock
    private PacienteMapper pacienteMapper;

    @Mock
    private TurnoRepositorio turnoRepositorio;

    @Mock
    private RecetaDigitalRepositorio recetaRepositorio;

    @Mock
    private HistoriaClinicaRepositorio historiaClinicaRepositorio;

    @Mock
    private EvolucionMedicaRepositorio evolucionMedicaRepositorio;

    @Mock
    private EstudioAdjuntoRepositorio estudioAdjuntoRepositorio;

    @Mock
    private AlergiaPacienteRepositorio alergiaPacienteRepositorio;

    @Mock
    private ConsentimientoInformadoRepositorio consentimientoRepositorio;

    @Mock
    private VinculoFamiliarRepositorio vinculoFamiliarRepositorio;

    @Mock
    private AuditoriaFusionPacienteRepositorio auditoriaFusionPacienteRepositorio;

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

    @Test
    void testFusionarPacientes_MigraTodosLosRegistrosYRevocaSesiones() {
        Paciente origen = new Paciente();
        origen.setId(10L);
        origen.setNombre("Juan");
        origen.setApellido("Pérez");
        origen.setDni("30111222");
        origen.setNroAfiliado("AF-10");
        Usuario usuarioOrigen = new Usuario();
        usuarioOrigen.setEstadoActivo(true);
        usuarioOrigen.setTokenVersion(1);
        origen.setUsuario(usuarioOrigen);

        Paciente destino = new Paciente();
        destino.setId(20L);
        destino.setNombre("Juan Carlos");
        destino.setApellido("Pérez");
        destino.setDni("30111222");

        when(pacienteRepositorio.findById(10L)).thenReturn(Optional.of(origen));
        when(pacienteRepositorio.findById(20L)).thenReturn(Optional.of(destino));

        List<Turno> turnos = new ArrayList<>();
        Turno turno = new Turno();
        turno.setId(100L);
        turnos.add(turno);
        when(turnoRepositorio.findByPacienteIdOrderByFechaHoraDesc(10L)).thenReturn(turnos);

        List<RecetaDigital> recetas = new ArrayList<>();
        RecetaDigital receta = new RecetaDigital();
        receta.setId(200L);
        recetas.add(receta);
        when(recetaRepositorio.findByPacienteIdOrderByFechaEmisionDesc(10L)).thenReturn(recetas);

        List<HistoriaClinica> historias = new ArrayList<>();
        HistoriaClinica hc = new HistoriaClinica();
        hc.setId(300L);
        historias.add(hc);
        when(historiaClinicaRepositorio.findByPacienteId(10L)).thenReturn(historias);

        List<EvolucionMedica> evoluciones = new ArrayList<>();
        EvolucionMedica ev = new EvolucionMedica();
        ev.setId(400L);
        evoluciones.add(ev);
        when(evolucionMedicaRepositorio.findByPacienteIdOrderByFechaCreacionDesc(10L)).thenReturn(evoluciones);

        List<EstudioAdjunto> estudios = new ArrayList<>();
        EstudioAdjunto est = new EstudioAdjunto();
        est.setId(500L);
        estudios.add(est);
        when(estudioAdjuntoRepositorio.findByPacienteIdOrderByFechaCargaDesc(10L)).thenReturn(estudios);

        List<AlergiaPaciente> alergias = new ArrayList<>();
        AlergiaPaciente al = new AlergiaPaciente();
        al.setId(600L);
        alergias.add(al);
        when(alergiaPacienteRepositorio.findByPacienteIdOrderByFechaRegistroDesc(10L)).thenReturn(alergias);

        List<ConsentimientoInformado> consentimientos = new ArrayList<>();
        ConsentimientoInformado cons = new ConsentimientoInformado();
        cons.setId(700L);
        consentimientos.add(cons);
        when(consentimientoRepositorio.findByPacienteIdOrderByFechaHoraFirmaDesc(10L)).thenReturn(consentimientos);

        List<VinculoFamiliar> vinculos = new ArrayList<>();
        VinculoFamiliar vinc = new VinculoFamiliar();
        vinc.setId(800L);
        vinculos.add(vinc);
        when(vinculoFamiliarRepositorio.findByTitularId(10L)).thenReturn(vinculos);

        List<VinculoFamiliar> vinculosFamiliar = new ArrayList<>();
        VinculoFamiliar vincFam = new VinculoFamiliar();
        vincFam.setId(801L);
        Paciente otroTitular = new Paciente();
        otroTitular.setId(99L);
        vincFam.setTitular(otroTitular);
        vincFam.setFamiliarDni("30111222");
        vinculosFamiliar.add(vincFam);
        when(vinculoFamiliarRepositorio.findByFamiliarDni("30111222")).thenReturn(vinculosFamiliar);

        pacienteServicio.fusionarPacientes(10L, 20L, "Fusión por DNI duplicado", "admin@esu.com");

        assertEquals(destino, turno.getPaciente());
        assertEquals(destino, receta.getPaciente());
        assertEquals(destino, hc.getPaciente());
        assertEquals(destino, ev.getPaciente());
        assertEquals(20L, est.getPacienteId());
        assertEquals(20L, al.getPacienteId());
        assertEquals(20L, cons.getPacienteId());
        assertEquals(destino, vinc.getTitular());
        assertEquals(destino.getDni(), vincFam.getFamiliarDni());

        assertFalse(usuarioOrigen.getEstadoActivo());
        assertEquals(2, usuarioOrigen.getTokenVersion());
        assertTrue(origen.getNombre().startsWith("[FUSIONADO #20]"));

        verify(pacienteRepositorio).save(origen);

        ArgumentCaptor<AuditoriaFusionPaciente> auditCaptor = ArgumentCaptor.forClass(AuditoriaFusionPaciente.class);
        verify(auditoriaFusionPacienteRepositorio).save(auditCaptor.capture());
        AuditoriaFusionPaciente audit = auditCaptor.getValue();
        assertEquals(10L, audit.getPacienteOrigenId());
        assertEquals(20L, audit.getPacienteDestinoId());
        assertEquals("admin@esu.com", audit.getOperadorEmail());
        assertNotNull(audit.getDetalles());
    }
}
