package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.DispensarRecetaDtoRequest;
import com.SaludUnificada.Esu.dto.request.MedicamentoItemDto;
import com.SaludUnificada.Esu.dto.request.RecetaDigitalDtoRequest;
import com.SaludUnificada.Esu.dto.response.DispensarRecetaDtoResponse;
import com.SaludUnificada.Esu.dto.response.RecetaDigitalDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.mapper.RecetaDigitalMapper;
import com.SaludUnificada.Esu.repositorio.LogAccesoHcRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.repositorio.VinculoFamiliarRepositorio;
import com.SaludUnificada.Esu.seguridad.OwnershipSecurityEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrigenAtencionesPortalPacienteTest {

    @Mock
    private RecetaDigitalRepositorio recetaRepositorio;
    @Mock
    private PacienteRepositorio pacienteRepositorio;
    @Mock
    private ProfesionalRepositorio profesionalRepositorio;
    @Mock
    private DocumentoPdfServicio pdfServicio;
    @Mock
    private EmailNotificacionServicio emailServicio;
    @Mock
    private AlergiaServicio alergiaServicio;
    @Spy
    private CatalogoFarmacologicoServicio catalogoFarmacologicoServicio = new CatalogoFarmacologicoServicio();
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;
    @Mock
    private TurnoRepositorio turnoRepositorio;
    @Mock
    private VinculoFamiliarRepositorio vinculoFamiliarRepositorio;
    @Mock
    private LogAccesoHcRepositorio logAccesoHcRepositorio;

    @InjectMocks
    private RecetaDigitalServicio recetaServicio;

    private RecetaDigitalMapper recetaMapper;
    private OwnershipSecurityEvaluator securityPolicy;

    private Paciente paciente;
    private Profesional profesional;

    @BeforeEach
    void setUp() {
        recetaMapper = new RecetaDigitalMapper();

        securityPolicy = new OwnershipSecurityEvaluator(
                usuarioRepositorio,
                pacienteRepositorio,
                null,
                profesionalRepositorio,
                turnoRepositorio,
                recetaRepositorio,
                null,
                null,
                null,
                null,
                logAccesoHcRepositorio,
                null,
                vinculoFamiliarRepositorio
        );

        Usuario userPac = new Usuario();
        userPac.setId(10L);
        userPac.setEmail("paciente@esu.com");
        userPac.setRol("PACIENTE");

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombre("Lucas");
        paciente.setApellido("Ramírez");
        paciente.setDni("38123456");
        paciente.setUsuario(userPac);
        paciente.setCentroSaludReferenciaId(1L);
        paciente.setCentroSaludReferenciaNombre("Hospital Regional Aurelio Crespo");

        Usuario userProf = new Usuario();
        userProf.setId(20L);
        userProf.setEmail("dr.carlos@esu.com");
        userProf.setRol("MEDICO");

        profesional = new Profesional();
        profesional.setId(2L);
        profesional.setNombre("Carlos");
        profesional.setApellido("Bustos");
        profesional.setMatricula("MP-45123");
        profesional.setEstadoActivo(true);
        profesional.setUsuario(userProf);
    }

    @Test
    @DisplayName("Fase 3: Emisión de receta registra el centro de salud emisor y lugar sugerido de retiro")
    void emitirReceta_debeRegistrarCentroSaludEmisorYLugarRetiro() {
        when(pacienteRepositorio.findById(1L)).thenReturn(Optional.of(paciente));
        when(profesionalRepositorio.findById(2L)).thenReturn(Optional.of(profesional));
        when(pdfServicio.calcularHashSha256(any())).thenReturn("hash-sha256-valido-123");
        when(recetaRepositorio.save(any(RecetaDigital.class))).thenAnswer(inv -> inv.getArgument(0));

        RecetaDigitalDtoRequest request = new RecetaDigitalDtoRequest();
        request.setPacienteId(1L);
        request.setProfesionalId(2L);
        request.setDiagnostico("Rinofaringitis aguda");
        request.setCentroSaludEmisorId(1L);
        request.setCentroSaludEmisorNombre("Hospital Regional Aurelio Crespo");
        request.setLugarRetiroSugerido("Farmacia Central Hospital Aurelio Crespo");
        MedicamentoItemDto med = new MedicamentoItemDto();
        med.setDroga("Paracetamol");
        med.setNombreComercial("Tafirol");
        med.setPresentacion("500mg");
        med.setCantidad(1);
        request.setMedicamentos(List.of(med));

        RecetaDigital emitida = recetaServicio.emitirReceta(request);

        assertNotNull(emitida);
        assertEquals(1L, emitida.getCentroSaludEmisorId());
        assertEquals("Hospital Regional Aurelio Crespo", emitida.getCentroSaludEmisorNombre());
        assertEquals("Farmacia Central Hospital Aurelio Crespo", emitida.getLugarRetiroSugerido());

        RecetaDigitalDtoResponse dto = recetaMapper.toDto(emitida);
        assertEquals(1L, dto.getCentroSaludEmisorId());
        assertEquals("Hospital Regional Aurelio Crespo", dto.getCentroSaludEmisorNombre());
        assertEquals("Farmacia Central Hospital Aurelio Crespo", dto.getLugarRetiroSugerido());
    }

    @Test
    @DisplayName("Fase 3: Dispensación farmacéutica registra nombre de la farmacia dispensadora independiente del centro emisor")
    void dispensarReceta_debeRegistrarFarmaciaDispensadoraSeparadaDeCentroEmisor() {
        RecetaDigital receta = new RecetaDigital();
        receta.setId(100L);
        receta.setPaciente(paciente);
        receta.setProfesional(profesional);
        receta.setEstadoReceta("PENDIENTE");
        receta.setFechaEmision(LocalDate.now());
        receta.setFechaVencimiento(LocalDate.now().plusDays(30));
        receta.setCentroSaludEmisorId(1L);
        receta.setCentroSaludEmisorNombre("Hospital Regional Aurelio Crespo");
        receta.setHashIntegridad("hash-receta-dispensa");

        when(recetaRepositorio.findByHashIntegridad("hash-receta-dispensa")).thenReturn(Optional.of(receta));
        when(recetaRepositorio.save(any(RecetaDigital.class))).thenAnswer(inv -> inv.getArgument(0));

        DispensarRecetaDtoRequest request = new DispensarRecetaDtoRequest(
                "hash-receta-dispensa",
                "FAR-8812",
                "Dispensación completa",
                "Farmacia Cruz del Eje Centro"
        );

        DispensarRecetaDtoResponse resp = recetaServicio.dispensarReceta(request);

        assertNotNull(resp);
        assertEquals("DISPENSADA", resp.getEstadoReceta());
        assertEquals("FAR-8812", resp.getMatriculaFarmacia());
        assertEquals("Farmacia Cruz del Eje Centro", resp.getFarmaciaNombre());
        assertEquals("Hospital Regional Aurelio Crespo", resp.getCentroSaludEmisorNombre());
        assertEquals(1L, resp.getCentroSaludEmisorId());
    }

    @Test
    @DisplayName("Fase 3: Paciente solo puede acceder a sus propios datos y se rechaza acceso a otros pacientes")
    void canAccessPatient_debeRechazarAccesoAOtroPacienteYPermitirElPropio() {
        SecurityContext ctx = mock(SecurityContext.class);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "paciente@esu.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_PACIENTE"))
        );
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        when(usuarioRepositorio.findByEmail("paciente@esu.com")).thenReturn(Optional.of(paciente.getUsuario()));
        when(pacienteRepositorio.findByUsuarioEmail("paciente@esu.com")).thenReturn(Optional.of(paciente));

        // 1. Acceso a su propio expediente (ID: 1) -> Permitido
        assertTrue(securityPolicy.canAccessPatient(1L));

        // 2. Acceso a otro expediente ajeno (ID: 99) sin vínculo familiar -> Denegado
        when(pacienteRepositorio.findById(99L)).thenReturn(Optional.empty());
        assertFalse(securityPolicy.canAccessPatient(99L));
    }

    @Test
    @DisplayName("Fase 3: Paciente con vínculo familiar activo puede acceder al expediente de su familiar")
    void canAccessPatient_conVinculoFamiliarActivoPermiteAcceso() {
        SecurityContext ctx = mock(SecurityContext.class);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "paciente@esu.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_PACIENTE"))
        );
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        when(usuarioRepositorio.findByEmail("paciente@esu.com")).thenReturn(Optional.of(paciente.getUsuario()));
        when(pacienteRepositorio.findByUsuarioEmail("paciente@esu.com")).thenReturn(Optional.of(paciente));

        Paciente hijo = new Paciente();
        hijo.setId(25L);
        hijo.setDni("55123456");
        when(pacienteRepositorio.findById(25L)).thenReturn(Optional.of(hijo));
        when(vinculoFamiliarRepositorio.existsByTitularIdAndFamiliarDni(1L, "55123456")).thenReturn(true);

        assertTrue(securityPolicy.canAccessPatient(25L));
    }
}
