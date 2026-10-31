package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.MetricasAusentismoDtoResponse;
import com.SaludUnificada.Esu.dto.response.TopDiagnosticoDtoResponse;
import com.SaludUnificada.Esu.repositorio.EvolucionMedicaRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.entidad.Turno;
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
import java.util.LinkedHashMap;
import java.util.EnumMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.Collectors;
import com.SaludUnificada.Esu.dto.response.DashboardMetricasDtoResponse;

@Service
public class MetricasSanitariasServicio {

    private static final Logger log = LoggerFactory.getLogger(MetricasSanitariasServicio.class);

    @Autowired
    private TurnoRepositorio turnoRepositorio;

    @Autowired
    private EvolucionMedicaRepositorio evolucionMedicaRepositorio;

    @Autowired(required = false)
    private PacienteRepositorio pacienteRepositorio;

    @Autowired(required = false)
    private ProfesionalRepositorio profesionalRepositorio;

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
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumenEjecutivo() {
        long totalPacientes = pacienteRepositorio != null ? pacienteRepositorio.count() : 0L;
        long totalProfesionales = profesionalRepositorio != null ? profesionalRepositorio.count() : 0L;
        long totalTurnos = turnoRepositorio != null ? turnoRepositorio.count() : 0L;
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = hoy.atTime(LocalTime.MAX);
        long totalTurnosMes = turnoRepositorio != null ? turnoRepositorio.contarTotalTurnosEntre(inicioMes, finMes) : 0L;

        Map<String, Object> res = new HashMap<>();
        res.put("totalPacientes", totalPacientes);
        res.put("totalProfesionales", totalProfesionales);
        res.put("totalTurnosHistoricos", totalTurnos);
        res.put("totalTurnosMesActual", totalTurnosMes);
        return res;
    }

    @Transactional(readOnly = true)
    public MetricasAusentismoDtoResponse obtenerMetricasAusentismoPorProfesional(Long profesionalId, LocalDate desde, LocalDate hasta) {
        LocalDate fDesde = (desde != null) ? desde : LocalDate.now().minusDays(30);
        LocalDate fHasta = (hasta != null) ? hasta : LocalDate.now();
        if (fDesde.isAfter(fHasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a hasta.");
        }
        LocalDateTime inicio = fDesde.atStartOfDay();
        LocalDateTime fin = fHasta.atTime(LocalTime.MAX);
        List<Turno> turnos = turnoRepositorio.findByProfesionalIdAndFechaHoraBetween(profesionalId, inicio, fin);
        long total = turnos.size();
        long atendidos = turnos.stream().filter(t -> "ATENDIDO".equalsIgnoreCase(t.getEstado()) || "FINALIZADO".equalsIgnoreCase(t.getEstado())).count();
        long ausentes = turnos.stream().filter(t -> "AUSENTE".equalsIgnoreCase(t.getEstado())).count();
        long cancelados = turnos.stream().filter(t -> t.getEstado() != null && t.getEstado().contains("CANCELADO")).count();
        long pendientes = total - atendidos - ausentes - cancelados;
        double tasaAusentismo = (total > 0) ? Math.round(((double) ausentes / total) * 10000.0) / 100.0 : 0.0;
        double tasaAsistencia = (total > 0) ? Math.round(((double) atendidos / total) * 10000.0) / 100.0 : 0.0;

        return MetricasAusentismoDtoResponse.builder()
                .desde(fDesde)
                .hasta(fHasta)
                .totalTurnos(total)
                .turnosAtendidos(atendidos)
                .turnosAusentes(ausentes)
                .turnosCancelados(cancelados)
                .turnosPendientes(pendientes)
                .tasaAusentismo(tasaAusentismo)
                .tasaAsistencia(tasaAsistencia)
                .mensajePeriodo("Metricas del profesional ID " + profesionalId)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardMetricasDtoResponse obtenerDashboardMetricas(LocalDate desde, LocalDate hasta, Long centroId, String especialidad) {
        LocalDate fDesde = (desde != null) ? desde : LocalDate.now().minusDays(30);
        LocalDate fHasta = (hasta != null) ? hasta : LocalDate.now();

        if (fDesde.isAfter(fHasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'.");
        }

        LocalDateTime inicio = fDesde.atStartOfDay();
        LocalDateTime fin = fHasta.atTime(LocalTime.MAX);

        String espFiltro = (especialidad != null && !especialidad.isBlank() && !"TODOS".equalsIgnoreCase(especialidad)) ? especialidad.trim() : null;
        List<Turno> turnos = turnoRepositorio.findTurnosParaMetricas(inicio, fin, centroId, espFiltro);

        long total = turnos.size();
        long atendidos = 0;
        long ausentes = 0;
        long cancelados = 0;
        long pendientes = 0;

        Map<String, Long> desglose = new LinkedHashMap<>();
        Map<String, Long> conteoEspecialidad = new HashMap<>();

        Map<DayOfWeek, long[]> conteoDiaSemana = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            conteoDiaSemana.put(d, new long[4]);
        }

        Map<YearMonth, long[]> conteoMensual = new TreeMap<>();

        ZoneId cdeZone = ZoneId.of("America/Argentina/Cordoba");
        LocalDate hoy = LocalDate.now(cdeZone);
        long turnosHoy = 0;
        long atendidosHoy = 0;
        long ausentesHoy = 0;

        for (Turno t : turnos) {
            String est = t.getEstado() != null ? t.getEstado().toUpperCase() : "RESERVADO";
            desglose.put(est, desglose.getOrDefault(est, 0L) + 1);

            boolean isAt = est.contains("ATENDIDO") || est.contains("COMPLETADO") || est.contains("FINALIZADO");
            boolean isAu = est.contains("AUSENTE") || est.contains("NO_ASISTIO");
            boolean isCanc = est.contains("CANCELADO");

            if (isAt) {
                atendidos++;
            } else if (isAu) {
                ausentes++;
            } else if (isCanc) {
                cancelados++;
            } else {
                pendientes++;
            }

            if (t.getFechaHora() != null && t.getFechaHora().toLocalDate().isEqual(hoy)) {
                turnosHoy++;
                if (isAt) atendidosHoy++;
                if (isAu) ausentesHoy++;
            }

            String esp = "Medicina General";
            if (t.getProfesional() != null && t.getProfesional().getEspecialidades() != null && !t.getProfesional().getEspecialidades().isEmpty()) {
                esp = t.getProfesional().getEspecialidades().iterator().next().getNombre();
            }
            conteoEspecialidad.put(esp, conteoEspecialidad.getOrDefault(esp, 0L) + 1);

            if (t.getFechaHora() != null) {
                DayOfWeek dow = t.getFechaHora().getDayOfWeek();
                long[] diaArr = conteoDiaSemana.get(dow);
                if (diaArr != null) {
                    diaArr[3]++;
                    if (isAt) diaArr[0]++;
                    if (isAu) diaArr[1]++;
                    if (isCanc) diaArr[2]++;
                }

                YearMonth ym = YearMonth.from(t.getFechaHora());
                long[] mesArr = conteoMensual.computeIfAbsent(ym, k -> new long[3]);
                mesArr[0]++;
                if (isAt) mesArr[1]++;
                if (isAu) mesArr[2]++;
            }
        }

        long concretados = atendidos + ausentes;
        double tasaAsistencia = concretados > 0 ? Math.round(((double) atendidos / concretados) * 1000.0) / 10.0 : 0.0;
        double tasaAusentismo = concretados > 0 ? Math.round(((double) ausentes / concretados) * 1000.0) / 10.0 : 0.0;
        double tasaCancelacion = total > 0 ? Math.round(((double) cancelados / total) * 1000.0) / 10.0 : 0.0;

        String topEsp = "Clínica Médica";
        long topEspCount = 0;
        for (Map.Entry<String, Long> e : conteoEspecialidad.entrySet()) {
            if (e.getValue() > topEspCount) {
                topEspCount = e.getValue();
                topEsp = e.getKey();
            }
        }

        String[] labelsDias = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        DayOfWeek[] dows = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};
        List<DashboardMetricasDtoResponse.DemandaDiaDto> demandaSemanalDto = new ArrayList<>();
        for (int i = 0; i < dows.length; i++) {
            long[] vals = conteoDiaSemana.get(dows[i]);
            demandaSemanalDto.add(DashboardMetricasDtoResponse.DemandaDiaDto.builder()
                    .diaSemana(dows[i].name())
                    .label(labelsDias[i])
                    .atendidos(vals[0])
                    .ausentes(vals[1])
                    .cancelados(vals[2])
                    .total(vals[3])
                    .build());
        }

        String[] colores = {"#0284c7", "#10b981", "#f59e0b", "#8b5cf6", "#ec4899", "#06b6d4", "#64748b"};
        List<DashboardMetricasDtoResponse.EspecialidadDemandaDto> espDemandaDto = new ArrayList<>();
        int colorIdx = 0;
        List<Map.Entry<String, Long>> sortedEsps = conteoEspecialidad.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(7)
                .collect(Collectors.toList());

        for (Map.Entry<String, Long> entry : sortedEsps) {
            double porc = total > 0 ? Math.round(((double) entry.getValue() / total) * 1000.0) / 10.0 : 0.0;
            espDemandaDto.add(DashboardMetricasDtoResponse.EspecialidadDemandaDto.builder()
                    .especialidad(entry.getKey())
                    .totalTurnos(entry.getValue())
                    .porcentaje(porc)
                    .color(colores[colorIdx % colores.length])
                    .build());
            colorIdx++;
        }

        List<DashboardMetricasDtoResponse.TendenciaMensualDto> tendenciaMensualDto = new ArrayList<>();
        for (Map.Entry<YearMonth, long[]> mEntry : conteoMensual.entrySet()) {
            long mTotal = mEntry.getValue()[0];
            long mAt = mEntry.getValue()[1];
            long mAu = mEntry.getValue()[2];
            long mConc = mAt + mAu;
            double mAsist = mConc > 0 ? Math.round(((double) mAt / mConc) * 1000.0) / 10.0 : 0.0;
            double mAus = mConc > 0 ? Math.round(((double) mAu / mConc) * 1000.0) / 10.0 : 0.0;
            String labelMes = mEntry.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-AR"));
            tendenciaMensualDto.add(DashboardMetricasDtoResponse.TendenciaMensualDto.builder()
                    .mes(mEntry.getKey().toString())
                    .label(labelMes.substring(0, 1).toUpperCase() + labelMes.substring(1))
                    .totalTurnos(mTotal)
                    .atendidos(mAt)
                    .ausentes(mAu)
                    .tasaAsistencia(mAsist)
                    .tasaAusentismo(mAus)
                    .build());
        }

        String periodoDesc = String.format("Período analizado: del %s al %s", fDesde, fHasta);
        String nombreCentro = "Todos los Centros (Red Cruz del Eje)";
        if (centroId != null) {
            periodoDesc += " (Centro ID: " + centroId + ")";
            nombreCentro = "Centro de Salud #" + centroId;
        }

        return DashboardMetricasDtoResponse.builder()
                .fechaDesde(fDesde)
                .fechaHasta(fHasta)
                .centroId(centroId)
                .centroNombre(nombreCentro)
                .especialidad(especialidad)
                .fechaActualizacion(LocalDateTime.now())
                .respuestaIncompleta(false)
                .periodoDescripcion(periodoDesc)
                .totalTurnos(total)
                .turnosAtendidos(atendidos)
                .turnosAusentes(ausentes)
                .turnosCancelados(cancelados)
                .turnosPendientes(pendientes)
                .turnosHoy(turnosHoy)
                .atendidosHoy(atendidosHoy)
                .ausentesHoy(ausentesHoy)
                .tasaAsistencia(tasaAsistencia)
                .tasaAusentismo(tasaAusentismo)
                .tasaCancelacion(tasaCancelacion)
                .tiempoPromedioConsultaMinutos(20.0)
                .especialidadMasDemandada(topEsp)
                .totalEspecialidadMasDemandada(topEspCount)
                .demandaSemanal(demandaSemanalDto)
                .distribucionEspecialidades(espDemandaDto)
                .evolucionMensual(tendenciaMensualDto)
                .desglosePorEstado(desglose)
                .build();
    }

}
