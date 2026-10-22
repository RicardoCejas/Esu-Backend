package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.*;
import com.SaludUnificada.Esu.dto.response.ResumenClinicoDtoResponse;
import com.SaludUnificada.Esu.dto.response.TimelineItemDto;
import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
            turno = turnoRepositorio.findById(request.getTurnoId())
                .orElseThrow(() -> new NoEncontradoExcepcion(
                    "Turno no encontrado con ID: " + request.getTurnoId()));

            if (evolucionRepositorio.findByTurnoId(request.getTurnoId()).isPresent()) {
                throw new IllegalStateException(
                    "Ya existe una evolucion clinica registrada para el turno " + request.getTurnoId() + ".");
            }
            if (turno.getPaciente() != null && !turno.getPaciente().getId().equals(paciente.getId())) {
                throw new IllegalArgumentException("El turno indicado no pertenece al paciente.");
            }
            if (turno.getProfesional() != null && !turno.getProfesional().getId().equals(profesional.getId())) {
                throw new IllegalArgumentException("El turno indicado no pertenece al profesional.");
            }

            turno.setEstado("FINALIZADO");
            turnoRepositorio.save(turno);
        }

        // Issue #120: la evolucion queda ligada a una historia clinica real del paciente.
        HistoriaClinica historiaClinica = new HistoriaClinica();
        historiaClinica.setPaciente(paciente);
        historiaClinica.setProfesional(profesional);
        historiaClinica.setMotivoConsulta(recortar(request.getMotivoConsulta()));
        historiaClinica.setDiagnostico(recortar(request.getDiagnostico()));
        historiaClinica.setTratamiento(recortar(request.getTratamiento()));
        historiaClinica.setFechaAtencion(LocalDateTime.now());
        historiaClinica = historiaClinicaRepositorio.save(historiaClinica);

        EvolucionMedica evolucion = new EvolucionMedica();
        evolucion.setHistoriaClinica(historiaClinica);
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

        // Signos vitales
        evolucion.setPresionArterial(request.getPresionArterial());
        evolucion.setFrecuenciaCardiaca(request.getFrecuenciaCardiaca());
        evolucion.setTemperatura(request.getTemperatura());
        evolucion.setPeso(request.getPeso());
        evolucion.setTalla(request.getTalla());
        evolucion.setImc(request.getImc());

        // Firma electrónica inalterable
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

        // Si incluye medicamentos prescriptos
        if (request.getMedicamentos() != null && !request.getMedicamentos().isEmpty()) {
            RecetaDigitalDtoRequest rRequest = new RecetaDigitalDtoRequest();
            rRequest.setPacienteId(paciente.getId());
            rRequest.setProfesionalId(profesional.getId());
            rRequest.setEvolucionId(guardada.getId());
            rRequest.setDiagnostico(request.getDiagnostico());
            rRequest.setCodigoCie10(request.getCodigoCie10());
            rRequest.setIndicacionesGenerales(request.getTratamiento());
            rRequest.setMedicamentos(request.getMedicamentos());
            RecetaDigital receta = recetaServicio.emitirReceta(rRequest);
            guardada.getRecetas().add(receta);
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
        dto.setObraSocial(paciente.getObraSocial());
        dto.setGrupoSanguineo(paciente.getGrupoSanguineo());
        dto.setTelefono(paciente.getTelefono());

        if (paciente.getFechaNacimiento() != null) {
            dto.setFechaNacimiento(paciente.getFechaNacimiento().toString());
            dto.setEdad(Period.between(paciente.getFechaNacimiento(), LocalDate.now()).getYears());
        }

        dto.setAlertasCriticas(dividirTexto(paciente.getAlergias()));
        dto.setAntecedentes(dividirTexto(paciente.getAntecedentes()));

        // Ultima evolucion real del paciente: sin valores de relleno.
        List<EvolucionMedica> evoluciones = evolucionRepositorio.findByPacienteIdOrderByFechaCreacionDesc(pacienteId);
        if (!evoluciones.isEmpty()) {
            EvolucionMedica ult = evoluciones.get(0);
            Map<String, String> vitales = new LinkedHashMap<>();
            agregarSiHay(vitales, "presionArterial", ult.getPresionArterial());
            agregarSiHay(vitales, "frecuenciaCardiaca", ult.getFrecuenciaCardiaca());
            agregarSiHay(vitales, "temperatura", ult.getTemperatura());
            agregarSiHay(vitales, "peso", ult.getPeso());
            agregarSiHay(vitales, "talla", ult.getTalla());
            agregarSiHay(vitales, "imc", ult.getImc());
            dto.setUltimosSignosVitales(vitales);

            LocalDateTime fechaConsulta = ult.getFechaFirma() != null ? ult.getFechaFirma() : ult.getFechaCreacion();
            dto.setUltimaConsultaFecha(fechaConsulta != null
                ? fechaConsulta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null);
            dto.setUltimoDiagnostico(ult.getDiagnostico());
        } else {
            dto.setUltimosSignosVitales(Collections.emptyMap());
        }

        return dto;
    }

    private List<String> dividirTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(texto.split("[;\\n]+"))
            .map(String::trim)
            .filter(valor -> !valor.isEmpty())
            .toList();
    }

    private void agregarSiHay(Map<String, String> destino, String clave, String valor) {
        if (valor != null && !valor.isBlank()) {
            destino.put(clave, valor);
        }
    }

    private String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= 255 ? texto : texto.substring(0, 255);
    }

    public List<TimelineItemDto> obtenerTimelinePaciente(Long pacienteId) {
        List<TimelineItemDto> timeline = new ArrayList<>();
        List<EvolucionMedica> evoluciones = evolucionRepositorio.findByPacienteIdOrderByFechaCreacionDesc(pacienteId);

        for (EvolucionMedica e : evoluciones) {
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

            timeline.add(item);
        }

        return timeline;
    }

    public byte[] generarPdfEvolucion(Long evolucionId) {
        EvolucionMedica e = evolucionRepositorio.findById(evolucionId)
            .orElseThrow(() -> new NoEncontradoExcepcion("Evolución médica no encontrada con ID: " + evolucionId));
        String qrUrl = frontendUrl + "/verificar-evolucion?hash=" + e.getHashIntegridad();
        return pdfServicio.generarEvolucionPdf(e, qrUrl);
    }
}
