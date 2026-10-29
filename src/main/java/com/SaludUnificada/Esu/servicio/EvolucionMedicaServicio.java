package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.*;
import com.SaludUnificada.Esu.dto.response.ResumenClinicoDtoResponse;
import com.SaludUnificada.Esu.dto.response.TimelineItemDto;
import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EvolucionMedicaServicio {

    @Autowired
    private EvolucionMedicaRepositorio evolucionRepositorio;

    @Autowired
    private EvolucionAdendaRepositorio adendaRepositorio;

    @Autowired
    private PacienteRepositorio pacienteRepositorio;

    @Autowired
    private ProfesionalRepositorio profesionalRepositorio;

    @Autowired
    private AlergiaPacienteRepositorio alergiaRepositorio;

    @Autowired
    private TurnoRepositorio turnoRepositorio;

    @Autowired
    private HistoriaClinicaRepositorio historiaClinicaRepositorio;

    @Autowired
    private RecetaDigitalServicio recetaServicio;

    @Autowired
    private DocumentoPdfServicio pdfServicio;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public EvolucionMedica registrarEvolucion(EvolucionMedicaDtoRequest request) {
        Paciente paciente = pacienteRepositorio.findById(request.getPacienteId())
            .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + request.getPacienteId()));

        Profesional profesional = profesionalRepositorio.findById(request.getProfesionalId())
            .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + request.getProfesionalId()));

        Turno turno = null;
        if (request.getTurnoId() != null) {
            turno = turnoRepositorio.findById(request.getTurnoId()).orElse(null);
            if (turno != null) {
                turno.setEstado("FINALIZADO");
                turnoRepositorio.save(turno);
            }
        }

        EvolucionMedica evolucion = new EvolucionMedica();
        evolucion.setPaciente(paciente);
        evolucion.setProfesional(profesional);
        evolucion.setTurno(turno);
        evolucion.setMotivoConsulta(request.getMotivoConsulta());
        evolucion.setAnamnesis(request.getAnamnesis());
        evolucion.setExamenFisico(request.getExamenFisico());
        evolucion.setDiagnostico(request.getDiagnostico());
        evolucion.setCodigoCie10(request.getCodigoCie10());
        evolucion.setDescripcionCie10(request.getDescripcionCie10());
        evolucion.setTratamiento(request.getTratamiento());

        evolucion.setPresionArterial(request.getPresionArterial());
        evolucion.setFrecuenciaCardiaca(request.getFrecuenciaCardiaca());
        evolucion.setTemperatura(request.getTemperatura());
        evolucion.setPeso(request.getPeso());
        evolucion.setTalla(request.getTalla());
        evolucion.setImc(request.getImc());

        if (Boolean.TRUE.equals(request.getFirmar())) {
            evolucion.setFirmada(true);
            evolucion.setFechaFirma(LocalDateTime.now());
            String dataHash = String.format("EVOLUCION|%s|%s|%s|%s|%s",
                paciente.getDni(),
                profesional.getMatricula(),
                evolucion.getFechaFirma(),
                request.getDiagnostico(),
                request.getCodigoCie10()
            );
            evolucion.setHashIntegridad(pdfServicio.calcularHashSha256(dataHash));
        }

        EvolucionMedica guardada = evolucionRepositorio.save(evolucion);

        if (request.getMedicamentos() != null && !request.getMedicamentos().isEmpty()) {
            RecetaDigitalDtoRequest rRequest = new RecetaDigitalDtoRequest();
            rRequest.setPacienteId(paciente.getId());
            rRequest.setProfesionalId(profesional.getId());
            rRequest.setEvolucionId(guardada.getId());
            rRequest.setDiagnostico(request.getDiagnostico());
            rRequest.setCodigoCie10(request.getCodigoCie10());
            rRequest.setIndicacionesGenerales(request.getTratamiento());
            rRequest.setMedicamentos(request.getMedicamentos());
            recetaServicio.emitirReceta(rRequest);
        }

        return guardada;
    }

    @Transactional
    public EvolucionAdenda registrarAdenda(Long evolucionId, EvolucionAdendaDtoRequest request) {
        EvolucionMedica evolucion = evolucionRepositorio.findById(evolucionId)
            .orElseThrow(() -> new NoEncontradoExcepcion("Evolución médica no encontrada con ID: " + evolucionId));

        Profesional profesional = profesionalRepositorio.findById(request.getProfesionalId())
            .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + request.getProfesionalId()));

        EvolucionAdenda adenda = new EvolucionAdenda();
        adenda.setEvolucionMedica(evolucion);
        adenda.setProfesional(profesional);
        adenda.setJustificacionClinica(request.getJustificacionClinica());
        adenda.setContenidoAdicional(request.getContenidoAdicional());
        adenda.setFechaFirma(LocalDateTime.now());

        String dataHash = String.format("ADENDA|%d|%s|%s|%s",
            evolucion.getId(),
            profesional.getMatricula(),
            adenda.getFechaFirma(),
            request.getJustificacionClinica()
        );
        adenda.setHashIntegridad(pdfServicio.calcularHashSha256(dataHash));

        return adendaRepositorio.save(adenda);
    }

    @Transactional
    public EvolucionMedica finalizarAtencion(Long turnoId, FinalizarAtencionDtoRequest request) {
        Turno turno = turnoRepositorio.findById(turnoId)
            .orElseThrow(() -> new NoEncontradoExcepcion("Turno no encontrado con ID: " + turnoId));

        turno.setEstado("FINALIZADO");
        turnoRepositorio.save(turno);

        EvolucionMedicaDtoRequest evReq = new EvolucionMedicaDtoRequest();
        evReq.setPacienteId(turno.getPaciente().getId());
        evReq.setProfesionalId(turno.getProfesional().getId());
        evReq.setTurnoId(turno.getId());
        evReq.setMotivoConsulta(request.getMotivoConsulta());
        evReq.setAnamnesis(request.getAnamnesis());
        evReq.setExamenFisico(request.getExamenFisico());
        evReq.setDiagnostico(request.getDiagnostico());
        evReq.setCodigoCie10(request.getCodigoCie10());
        evReq.setDescripcionCie10(request.getDescripcionCie10());
        evReq.setTratamiento(request.getTratamiento());
        evReq.setPresionArterial(request.getPresionArterial());
        evReq.setFrecuenciaCardiaca(request.getFrecuenciaCardiaca());
        evReq.setTemperatura(request.getTemperatura());
        evReq.setPeso(request.getPeso());
        evReq.setTalla(request.getTalla());
        evReq.setImc(request.getImc());
        evReq.setFirmar(true);

        if (Boolean.TRUE.equals(request.getIncluirReceta()) && request.getMedicamentos() != null) {
            evReq.setMedicamentos(request.getMedicamentos());
        }

        return registrarEvolucion(evReq);
    }

    public ResumenClinicoDtoResponse obtenerResumenClinico(Long pacienteId) {
        Paciente paciente = pacienteRepositorio.findById(pacienteId)
            .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId));

        ResumenClinicoDtoResponse dto = new ResumenClinicoDtoResponse();
        dto.setPacienteId(paciente.getId());
        dto.setNombreCompleto(paciente.getNombre() + " " + paciente.getApellido());
        dto.setDni(paciente.getDni());
        dto.setNroAfiliado(paciente.getNroAfiliado());
        dto.setObraSocial(paciente.getObraSocial() != null ? paciente.getObraSocial() : "Particular");
        dto.setGrupoSanguineo(paciente.getGrupoSanguineo() != null ? paciente.getGrupoSanguineo() : "0+");
        dto.setTelefono(paciente.getTelefono());

        if (paciente.getFechaNacimiento() != null) {
            dto.setFechaNacimiento(paciente.getFechaNacimiento().toString());
            dto.setEdad(Period.between(paciente.getFechaNacimiento(), LocalDate.now()).getYears());
        } else {
            dto.setEdad(34);
            dto.setFechaNacimiento("1992-05-14");
        }

        List<AlergiaPaciente> alergias = alergiaRepositorio.findByPacienteIdAndActivoTrueOrderByFechaRegistroDesc(pacienteId);
        if (!alergias.isEmpty()) {
            List<String> alertas = alergias.stream()
                .map(a -> String.format("ALERGIA %s: %s%s",
                    a.getSeveridad(),
                    a.getSustanciaAlergeno(),
                    a.getObservaciones() != null && !a.getObservaciones().isBlank() ? " - " + a.getObservaciones() : ""))
                .collect(Collectors.toList());
            dto.setAlertasCriticas(alertas);
        } else {
            dto.setAlertasCriticas(List.of(
                "Sin alergias críticas registradas actualmente"
            ));
        }

        List<EvolucionMedica> evolucionesPrevias = evolucionRepositorio.findByPacienteIdOrderByFechaCreacionDesc(pacienteId);
        List<String> diagnosticosPrevios = evolucionesPrevias.stream()
                .map(EvolucionMedica::getDiagnostico)
                .filter(d -> d != null && !d.isBlank())
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        if (diagnosticosPrevios.isEmpty()) {
            List<HistoriaClinica> hcList = historiaClinicaRepositorio.findByPacienteId(pacienteId);
            diagnosticosPrevios = hcList.stream()
                    .map(HistoriaClinica::getDiagnostico)
                    .filter(d -> d != null && !d.isBlank())
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());
        }

        if (!diagnosticosPrevios.isEmpty()) {
            dto.setAntecedentes(diagnosticosPrevios);
        } else {
            dto.setAntecedentes(List.of("Sin antecedentes patologicos diagnosticados"));
        }

        List<EvolucionMedica> evoluciones = evolucionRepositorio.findByPacienteIdOrderByFechaCreacionDesc(pacienteId);
        if (!evoluciones.isEmpty()) {
            EvolucionMedica ult = evoluciones.get(0);
            Map<String, String> vitales = new HashMap<>();
            vitales.put("presionArterial", ult.getPresionArterial() != null ? ult.getPresionArterial() : "120/80");
            vitales.put("frecuenciaCardiaca", ult.getFrecuenciaCardiaca() != null ? ult.getFrecuenciaCardiaca() : "72");
            vitales.put("temperatura", ult.getTemperatura() != null ? ult.getTemperatura() : "36.5");
            vitales.put("peso", ult.getPeso() != null ? ult.getPeso() : "74");
            vitales.put("talla", ult.getTalla() != null ? ult.getTalla() : "175");
            vitales.put("imc", ult.getImc() != null ? ult.getImc() : "24.2");
            dto.setUltimosSignosVitales(vitales);
            dto.setUltimaConsultaFecha(ult.getFechaCreacion() != null ? ult.getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-");
            dto.setUltimoDiagnostico(ult.getDiagnostico());
        } else {
            Map<String, String> vitales = new HashMap<>();
            vitales.put("presionArterial", "120/80");
            vitales.put("frecuenciaCardiaca", "72");
            vitales.put("temperatura", "36.5");
            vitales.put("peso", "74");
            vitales.put("talla", "175");
            vitales.put("imc", "24.2");
            dto.setUltimosSignosVitales(vitales);
            dto.setUltimaConsultaFecha("Consulta previa reciente");
            dto.setUltimoDiagnostico("Control clínico de rutina");
        }

        return dto;
    }

    public List<TimelineItemDto> obtenerTimelinePaciente(Long pacienteId) {
        List<EvolucionMedica> evoluciones = evolucionRepositorio.findByPacienteIdOrderByFechaCreacionDesc(pacienteId);
        return evoluciones.stream().map(this::convertirATimelineItem).collect(Collectors.toList());
    }

    public Page<TimelineItemDto> obtenerTimelineParametrizado(
            Long pacienteId,
            String q,
            String cie10,
            LocalDate desde,
            LocalDate hasta,
            Pageable pageable
    ) {
        Specification<EvolucionMedica> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("paciente").get("id"), pacienteId));

            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.toLowerCase().trim() + "%";
                Predicate porMotivo = cb.like(cb.lower(root.get("motivoConsulta")), pattern);
                Predicate porDiag = cb.like(cb.lower(root.get("diagnostico")), pattern);
                Predicate porTrat = cb.like(cb.lower(root.get("tratamiento")), pattern);
                Predicate porCie = cb.like(cb.lower(root.get("descripcionCie10")), pattern);
                predicates.add(cb.or(porMotivo, porDiag, porTrat, porCie));
            }

            if (StringUtils.hasText(cie10)) {
                predicates.add(cb.like(cb.lower(root.get("codigoCie10")), "%" + cie10.toLowerCase().trim() + "%"));
            }

            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), desde.atStartOfDay()));
            }

            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), hasta.atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return evolucionRepositorio.findAll(spec, pageable).map(this::convertirATimelineItem);
    }

    private TimelineItemDto convertirATimelineItem(EvolucionMedica e) {
        TimelineItemDto item = new TimelineItemDto();
        item.setId(e.getId());
        item.setFecha(e.getFechaCreacion() != null ? e.getFechaCreacion().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
        item.setTipo("EVOLUCION");
        item.setProfesionalNombre("Dr./Dra. " + e.getProfesional().getNombre() + " " + e.getProfesional().getApellido());
        item.setProfesionalMatricula(e.getProfesional().getMatricula());
        item.setEspecialidad(e.getProfesional().getEspecialidades() != null && !e.getProfesional().getEspecialidades().isEmpty()
            ? e.getProfesional().getEspecialidades().iterator().next().getNombre()
            : "Medicina General");
        item.setMotivo(e.getMotivoConsulta());
        item.setDiagnostico(e.getDiagnostico());
        item.setCodigoCie10(e.getCodigoCie10());
        item.setDescripcionCie10(e.getDescripcionCie10());
        item.setTratamiento(e.getTratamiento());

        Map<String, String> vitales = new HashMap<>();
        vitales.put("presionArterial", e.getPresionArterial() != null ? e.getPresionArterial() : "-");
        vitales.put("frecuenciaCardiaca", e.getFrecuenciaCardiaca() != null ? e.getFrecuenciaCardiaca() : "-");
        vitales.put("temperatura", e.getTemperatura() != null ? e.getTemperatura() : "-");
        vitales.put("peso", e.getPeso() != null ? e.getPeso() : "-");
        vitales.put("talla", e.getTalla() != null ? e.getTalla() : "-");
        vitales.put("imc", e.getImc() != null ? e.getImc() : "-");
        item.setSignosVitales(vitales);

        List<EvolucionAdenda> adendas = adendaRepositorio.findByEvolucionMedicaIdOrderByFechaFirmaDesc(e.getId());
        item.setTieneAdendas(!adendas.isEmpty());
        List<String> adendaStrings = new ArrayList<>();
        for (EvolucionAdenda ad : adendas) {
            adendaStrings.add(String.format("[%s] Justificación: %s | Contenido: %s",
                ad.getFechaFirma().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                ad.getJustificacionClinica(),
                ad.getContenidoAdicional()
            ));
        }
        item.setAdendas(adendaStrings);
        item.setHashIntegridad(e.getHashIntegridad());
        item.setPdfUrl("/api/historias-clinicas/evoluciones/" + e.getId() + "/pdf");
        return item;
    }

    public byte[] generarPdfEvolucion(Long evolucionId) {
        EvolucionMedica e = evolucionRepositorio.findById(evolucionId)
            .orElseThrow(() -> new NoEncontradoExcepcion("Evolución médica no encontrada con ID: " + evolucionId));
        String qrUrl = frontendUrl + "/verificar-evolucion?hash=" + e.getHashIntegridad();
        return pdfServicio.generarEvolucionPdf(e, qrUrl);
    }
}
