package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.MetricasAusentismoDtoResponse;
import com.SaludUnificada.Esu.dto.response.TopDiagnosticoDtoResponse;
import com.SaludUnificada.Esu.repositorio.EvolucionMedicaRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricasSanitariasServicio {

    private static final Logger log = LoggerFactory.getLogger(MetricasSanitariasServicio.class);

    @Autowired
    private TurnoRepositorio turnoRepositorio;

    @Autowired
    private EvolucionMedicaRepositorio evolucionMedicaRepositorio;

    @Transactional(readOnly = true)
    public MetricasAusentismoDtoResponse obtenerMetricasAusentismo(LocalDate desde, LocalDate hasta, Long centroId) {
        LocalDate fDesde = (desde != null) ? desde : LocalDate.now().minusDays(30);
        LocalDate fHasta = (hasta != null) ? hasta : LocalDate.now();

        if (fDesde.isAfter(fHasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'.");
        }

        LocalDateTime inicio = fDesde.atStartOfDay();
        LocalDateTime fin = fHasta.atTime(LocalTime.MAX);

        List<Object[]> resultados = turnoRepositorio.contarTurnosPorEstadoYFechaHoraEntre(inicio, fin);

        Map<String, Long> desglose = new HashMap<>();
        long totalTurnos = 0;
        long turnosAtendidos = 0;
        long turnosAusentes = 0;
        long turnosCancelados = 0;
        long turnosPendientes = 0;

        for (Object[] fila : resultados) {
            String estado = (fila[0] != null) ? fila[0].toString().toUpperCase() : "DESCONOCIDO";
            Long cantidad = (fila[1] != null) ? ((Number) fila[1]).longValue() : 0L;
            desglose.put(estado, cantidad);
            totalTurnos += cantidad;

            if (estado.contains("ATENDIDO") || estado.contains("FINALIZADO") || estado.contains("COMPLETADO")) {
                turnosAtendidos += cantidad;
            } else if (estado.contains("AUSENTE") || estado.contains("NO_ASISTIO")) {
                turnosAusentes += cantidad;
            } else if (estado.contains("CANCELADO")) {
                turnosCancelados += cantidad;
            } else {
                turnosPendientes += cantidad;
            }
        }

        double tasaAusentismo = (totalTurnos > 0) ? Math.round(((double) turnosAusentes / totalTurnos) * 10000.0) / 100.0 : 0.0;
        double tasaAsistencia = (totalTurnos > 0) ? Math.round(((double) turnosAtendidos / totalTurnos) * 10000.0) / 100.0 : 0.0;

        String periodo = String.format("Período analizado: del %s al %s", fDesde, fHasta);
        if (centroId != null) {
            periodo += " (Centro de Salud ID: " + centroId + ")";
        }

        log.info("Métricas calculadas: total={}, ausentes={} ({}%), atendidos={} ({}%)",
                totalTurnos, turnosAusentes, tasaAusentismo, turnosAtendidos, tasaAsistencia);

        return MetricasAusentismoDtoResponse.builder()
                .desde(fDesde)
                .hasta(fHasta)
                .centroId(centroId)
                .totalTurnos(totalTurnos)
                .turnosAtendidos(turnosAtendidos)
                .turnosAusentes(turnosAusentes)
                .turnosCancelados(turnosCancelados)
                .turnosPendientes(turnosPendientes)
                .tasaAusentismo(tasaAusentismo)
                .tasaAsistencia(tasaAsistencia)
                .desglosePorEstado(desglose)
                .mensajePeriodo(periodo)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TopDiagnosticoDtoResponse> obtenerTopDiagnosticos(int limit) {
        int limite = (limit > 0) ? Math.min(limit, 100) : 10;
        List<TopDiagnosticoDtoResponse> tops = evolucionMedicaRepositorio.obtenerTopDiagnosticosCie10(PageRequest.of(0, limite));

        long totalConDiagnostico = evolucionMedicaRepositorio.contarTotalEvolucionesConCie10();

        if (totalConDiagnostico > 0) {
            for (TopDiagnosticoDtoResponse item : tops) {
                double porcentaje = (item.getTotalCasos() != null)
                        ? Math.round(((double) item.getTotalCasos() / totalConDiagnostico) * 10000.0) / 100.0
                        : 0.0;
                item.setPorcentaje(porcentaje);
            }
        }

        log.info("Top {} diagnósticos CIE-10 consultados satisfactoriamente.", tops.size());
        return tops;
    }
}
