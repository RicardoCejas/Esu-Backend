package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.MetricasAusentismoDtoResponse;
import com.SaludUnificada.Esu.dto.response.TopDiagnosticoDtoResponse;
import com.SaludUnificada.Esu.repositorio.EvolucionMedicaRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricasSanitariasServicioTest {

    @Mock
    private TurnoRepositorio turnoRepositorio;

    @Mock
    private EvolucionMedicaRepositorio evolucionMedicaRepositorio;

    @InjectMocks
    private MetricasSanitariasServicio metricasServicio;

    @Test
    void obtenerMetricasAusentismo_calculaTasasCorrectamente() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);

        List<Object[]> resultados = new ArrayList<>();
        resultados.add(new Object[]{"ATENDIDO", 60L});
        resultados.add(new Object[]{"AUSENTE", 20L});
        resultados.add(new Object[]{"CANCELADO", 10L});
        resultados.add(new Object[]{"CONFIRMADO", 10L});

        when(turnoRepositorio.contarTurnosPorEstadoYFechaHoraEntre(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(resultados);

        MetricasAusentismoDtoResponse response = metricasServicio.obtenerMetricasAusentismo(desde, hasta, null);

        assertNotNull(response);
        assertEquals(100L, response.getTotalTurnos());
        assertEquals(60L, response.getTurnosAtendidos());
        assertEquals(20L, response.getTurnosAusentes());
        assertEquals(10L, response.getTurnosCancelados());
        assertEquals(10L, response.getTurnosPendientes());
        assertEquals(20.0, response.getTasaAusentismo());
        assertEquals(60.0, response.getTasaAsistencia());
    }

    @Test
    void obtenerTopDiagnosticos_calculaPorcentajesCorrectamente() {
        List<TopDiagnosticoDtoResponse> tops = new ArrayList<>();
        tops.add(new TopDiagnosticoDtoResponse("I10", "Hipertensión esencial (primaria)", 50L));
        tops.add(new TopDiagnosticoDtoResponse("E11", "Diabetes mellitus tipo 2", 25L));

        when(evolucionMedicaRepositorio.obtenerTopDiagnosticosCie10(any(Pageable.class))).thenReturn(tops);
        when(evolucionMedicaRepositorio.contarTotalEvolucionesConCie10()).thenReturn(100L);

        List<TopDiagnosticoDtoResponse> res = metricasServicio.obtenerTopDiagnosticos(10);

        assertEquals(2, res.size());
        assertEquals(50.0, res.get(0).getPorcentaje());
        assertEquals(25.0, res.get(1).getPorcentaje());
    }
}
