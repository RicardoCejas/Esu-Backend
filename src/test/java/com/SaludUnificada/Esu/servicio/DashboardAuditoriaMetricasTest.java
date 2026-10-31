package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.DashboardMetricasDtoResponse;
import com.SaludUnificada.Esu.dto.response.LogAccesoHcDtoResponse;
import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.entidad.LogAccesoHc;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.EvolucionMedicaRepositorio;
import com.SaludUnificada.Esu.repositorio.LogAccesoHcRepositorio;
import com.SaludUnificada.Esu.repositorio.LogAuditoriaSeguridadRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.RequestIpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardAuditoriaMetricasTest {

    @Mock
    private TurnoRepositorio turnoRepositorio;

    @Mock
    private EvolucionMedicaRepositorio evolucionMedicaRepositorio;

    @Mock
    private LogAccesoHcRepositorio logAccesoHcRepositorio;

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private LogAuditoriaSeguridadRepositorio logAuditoriaSeguridadRepositorio;

    @Mock
    private RequestIpUtil requestIpUtil;

    @InjectMocks
    private MetricasSanitariasServicio metricasServicio;

    private AuditoriaSeguridadServicio auditoriaSeguridadServicio;

    @BeforeEach
    void setUp() {
        auditoriaSeguridadServicio = new AuditoriaSeguridadServicio(
                logAuditoriaSeguridadRepositorio,
                requestIpUtil,
                logAccesoHcRepositorio,
                usuarioRepositorio
        );
    }

    @Test
    @DisplayName("Fase 4: Dashboard calcula métricas agregadas reales con denominadores precisos sin inventar valores")
    void obtenerDashboardMetricas_debeCalcularMetricasRealesConMetadatos() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);

        Especialidad cardiologia = new Especialidad();
        cardiologia.setId(10L);
        cardiologia.setNombre("Cardiología");

        Profesional prof = new Profesional();
        prof.setId(1L);
        prof.setNombre("Carlos");
        prof.setApellido("Bustos");
        prof.setEspecialidades(Set.of(cardiologia));

        Turno t1 = new Turno();
        t1.setId(101L);
        t1.setFechaHora(LocalDateTime.of(2026, 10, 15, 9, 0));
        t1.setEstado("COMPLETADO");
        t1.setCentroSaludId(1L);
        t1.setProfesional(prof);

        Turno t2 = new Turno();
        t2.setId(102L);
        t2.setFechaHora(LocalDateTime.of(2026, 10, 15, 10, 0));
        t2.setEstado("AUSENTE");
        t2.setCentroSaludId(1L);
        t2.setProfesional(prof);

        Turno t3 = new Turno();
        t3.setId(103L);
        t3.setFechaHora(LocalDateTime.of(2026, 10, 16, 11, 0));
        t3.setEstado("CANCELADO");
        t3.setCentroSaludId(1L);
        t3.setProfesional(prof);

        Turno t4 = new Turno();
        t4.setId(104L);
        t4.setFechaHora(LocalDateTime.of(2026, 10, 16, 12, 0));
        t4.setEstado("RESERVADO");
        t4.setCentroSaludId(1L);
        t4.setProfesional(prof);

        when(turnoRepositorio.findTurnosParaMetricas(any(), any(), eq(1L), isNull()))
                .thenReturn(List.of(t1, t2, t3, t4));

        DashboardMetricasDtoResponse res = metricasServicio.obtenerDashboardMetricas(desde, hasta, 1L, null);

        assertNotNull(res);
        assertEquals(4L, res.getTotalTurnos());
        assertEquals(1L, res.getTurnosAtendidos());
        assertEquals(1L, res.getTurnosAusentes());
        assertEquals(1L, res.getTurnosCancelados());
        assertEquals(1L, res.getTurnosPendientes());

        // Concretados = atendidos (1) + ausentes (1) = 2
        // Tasa Asistencia = 1 / 2 = 50.0%
        // Tasa Ausentismo = 1 / 2 = 50.0%
        // Tasa Cancelación = 1 / 4 = 25.0%
        assertEquals(50.0, res.getTasaAsistencia());
        assertEquals(50.0, res.getTasaAusentismo());
        assertEquals(25.0, res.getTasaCancelacion());

        assertNotNull(res.getFechaActualizacion());
        assertEquals(desde, res.getFechaDesde());
        assertEquals(hasta, res.getFechaHasta());
        assertFalse(res.getRespuestaIncompleta());
        assertEquals("Cardiología", res.getEspecialidadMasDemandada());
    }

    @Test
    @DisplayName("Fase 4: Periodo vacío devuelve tasas en 0.0 y no valores inventados por defecto")
    void obtenerDashboardMetricas_periodoVacio_debeDevolverCerosSinValoresFicticios() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);

        when(turnoRepositorio.findTurnosParaMetricas(any(), any(), isNull(), isNull()))
                .thenReturn(List.of());

        DashboardMetricasDtoResponse res = metricasServicio.obtenerDashboardMetricas(desde, hasta, null, null);

        assertNotNull(res);
        assertEquals(0L, res.getTotalTurnos());
        assertEquals(0.0, res.getTasaAusentismo());
        assertEquals(0.0, res.getTasaAsistencia());
        assertEquals(0.0, res.getTasaCancelacion());
    }

    @Test
    @DisplayName("Fase 4: Auditoría de accesos clínicos para administrador enmascara paciente y no expone diagnósticos")
    void listarAccesosClinicos_debeAnonimizarDatosYConservarTrazabilidad() {
        LogAccesoHc logItem = new LogAccesoHc(
                5L,
                42L,
                "CONSULTA_HC",
                "192.168.1.50",
                "ROLE_MEDICO",
                "Consulta programada de guardia"
        );
        logItem.setId(999L);

        Usuario medico = new Usuario();
        medico.setId(5L);
        medico.setEmail("dr.carlos@hospitalcrespo.gob.ar");

        when(logAccesoHcRepositorio.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(logItem)));
        when(usuarioRepositorio.findById(5L)).thenReturn(Optional.of(medico));

        Page<LogAccesoHcDtoResponse> page = auditoriaSeguridadServicio.listarAccesosClinicos(PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());

        LogAccesoHcDtoResponse dto = page.getContent().get(0);
        assertEquals(999L, dto.getId());
        assertEquals("d***s@h***.ar", dto.getUsuarioEmail());
        assertEquals("PAC-#42", dto.getPacienteIdentificador());
        assertEquals("CONSULTA_HC", dto.getAccion());
        assertEquals("Consulta programada de guardia", dto.getMotivo());
        assertEquals("192.168.1.50", dto.getIpAddress());
        assertNotNull(dto.getCentroCustodia());
    }
}
