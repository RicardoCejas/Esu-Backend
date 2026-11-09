package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    void testActualizarMiPerfilPorEmail_PreservaGuionYPersisteTodosLosCampos() {
        Paciente paciente = new Paciente();
        paciente.setId(31L);
        paciente.setNombre("Ricardo");
        paciente.setApellido("Cejas");
        paciente.setDni("G-12345678");
        paciente.setNroAfiliado("GGL-12345678");

        when(pacienteRepositorio.findByUsuarioEmail("ricardo@gmail.com")).thenReturn(Optional.of(paciente));
        when(pacienteRepositorio.findByDni("40026880")).thenReturn(Optional.empty());
        when(pacienteRepositorio.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pacienteMapper.paraDto(any(Paciente.class))).thenAnswer(inv -> {
            Paciente p = inv.getArgument(0);
            PacienteDtoResponse dto = new PacienteDtoResponse();
            dto.setId(p.getId());
            dto.setNombre(p.getNombre());
            dto.setApellido(p.getApellido());
            dto.setDni(p.getDni());
            dto.setTelefono(p.getTelefono());
            dto.setFechaNacimiento(p.getFechaNacimiento());
            dto.setObraSocial(p.getObraSocial());
            dto.setNroAfiliado(p.getNroAfiliado());
            dto.setGrupoSanguineo(p.getGrupoSanguineo());
            dto.setCentroSaludReferenciaNombre(p.getCentroSaludReferenciaNombre());
            return dto;
        });

        Map<String, String> body = new HashMap<>();
        body.put("nombre", "Ricardo");
        body.put("apellido", "Cejas");
        body.put("dni", "40026880");
        body.put("telefono", "3549-123456");
        body.put("fechaNacimiento", "14/08/1993");
        body.put("obraSocial", "APROSS (Córdoba)");
        body.put("nroAfiliado", "20-40026880-4/01");
        body.put("grupoSanguineo", "A+");
        body.put("centroSaludReferenciaNombre", "Hospital Provincial Dr. Aurelio Crespo");

        PacienteDtoResponse res = pacienteServicio.actualizarMiPerfilPorEmail("ricardo@gmail.com", body);

        assertEquals("40026880", res.getDni());
        assertEquals("3549-123456", res.getTelefono());
        assertEquals(LocalDate.of(1993, 8, 14), res.getFechaNacimiento());
        assertEquals("APROSS (Córdoba)", res.getObraSocial());
        assertEquals("20-40026880-4/01", res.getNroAfiliado());
        assertEquals("A+", res.getGrupoSanguineo());
        assertEquals("Hospital Provincial Dr. Aurelio Crespo", res.getCentroSaludReferenciaNombre());
    }

    @Test
    void testActualizarMiPerfilPorEmail_NoSobrescribeTelefonoConCadenaVacia() {
        Paciente paciente = new Paciente();
        paciente.setId(31L);
        paciente.setNombre("Ricardo");
        paciente.setApellido("Cejas");
        paciente.setDni("40026880");
        paciente.setTelefono("3549-654321");
        paciente.setFechaNacimiento(LocalDate.of(1992, 5, 18));

        when(pacienteRepositorio.findByUsuarioEmail("ricardo@gmail.com")).thenReturn(Optional.of(paciente));
        when(pacienteRepositorio.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pacienteMapper.paraDto(any(Paciente.class))).thenAnswer(inv -> {
            Paciente p = inv.getArgument(0);
            PacienteDtoResponse dto = new PacienteDtoResponse();
            dto.setTelefono(p.getTelefono());
            dto.setFechaNacimiento(p.getFechaNacimiento());
            return dto;
        });

        Map<String, String> body = new HashMap<>();
        body.put("telefono", "   ");
        body.put("obraSocial", "Particular");

        PacienteDtoResponse res = pacienteServicio.actualizarMiPerfilPorEmail("ricardo@gmail.com", body);

        assertEquals("3549-654321", res.getTelefono());
        assertEquals(LocalDate.of(1992, 5, 18), res.getFechaNacimiento());
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
