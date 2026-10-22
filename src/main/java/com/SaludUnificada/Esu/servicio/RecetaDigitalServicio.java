package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.MedicamentoItemDto;
import com.SaludUnificada.Esu.dto.request.RecetaDigitalDtoRequest;
import com.SaludUnificada.Esu.entidad.EvolucionMedica;
import com.SaludUnificada.Esu.entidad.MedicamentoReceta;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.EvolucionMedicaRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecetaDigitalServicio {

    @Autowired
    private RecetaDigitalRepositorio recetaRepositorio;

    @Autowired
    private PacienteRepositorio pacienteRepositorio;

    @Autowired
    private ProfesionalRepositorio profesionalRepositorio;

    @Autowired
    private EvolucionMedicaRepositorio evolucionMedicaRepositorio;

    @Autowired
    private DocumentoPdfServicio pdfServicio;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public RecetaDigital emitirReceta(RecetaDigitalDtoRequest request) {
        validarProfesionalHabilitado(request.getProfesionalId());

        Paciente paciente = pacienteRepositorio.findById(request.getPacienteId())
            .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + request.getPacienteId()));

        Profesional profesional = profesionalRepositorio.findById(request.getProfesionalId())
            .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + request.getProfesionalId()));

        EvolucionMedica evolucion = resolverEvolucion(request, paciente, profesional);

        RecetaDigital receta = new RecetaDigital();
        receta.setPaciente(paciente);
        receta.setProfesional(profesional);
        receta.setEvolucionMedica(evolucion);
        receta.setFechaEmision(LocalDate.now());
        receta.setFechaVencimiento(LocalDate.now().plusDays(30));
        receta.setDiagnostico(request.getDiagnostico());
        receta.setCodigoCie10(request.getCodigoCie10());
        receta.setIndicacionesGenerales(request.getIndicacionesGenerales());

        List<MedicamentoReceta> meds = new ArrayList<>();
        if (request.getMedicamentos() != null) {
            for (MedicamentoItemDto item : request.getMedicamentos()) {
                MedicamentoReceta m = new MedicamentoReceta();
                m.setReceta(receta);
                m.setNombreComercial(item.getNombreComercial());
                m.setDroga(item.getDroga());
                m.setPresentacion(item.getPresentacion());
                m.setDosisIndicada(item.getDosisIndicada());
                m.setPosologia(item.getPosologia());
                m.setViaAdministracion(item.getViaAdministracion());
                m.setDuracion(item.getDuracion());
                m.setCantidad(item.getCantidad());
                meds.add(m);
            }
        }
        receta.setMedicamentos(meds);

        // Generar hash SHA-256 de integridad antes de persistir
        String textoParaHash = String.format("RECETA|%s|%s|%s|%s|%s",
            paciente.getDni(),
            profesional.getMatricula(),
            receta.getFechaEmision(),
            receta.getDiagnostico(),
            request.getMedicamentos() != null ? request.getMedicamentos().size() : 0
        );
        String hash = pdfServicio.calcularHashSha256(textoParaHash);
        receta.setHashIntegridad(hash);

        String qrUrl = frontendUrl + "/verificar-receta?hash=" + hash;
        receta.setCodigoQrUrl(qrUrl);

        return recetaRepositorio.save(receta);
    }

    /**
     * Issue #120: validacion real de la prescripcion. Devuelve {@code valido=false}
     * con un motivo concreto en lugar de aceptar cualquier payload.
     */
    public Map<String, Object> validarPrescripcion(RecetaDigitalDtoRequest request) {
        Map<String, Object> resultado = new HashMap<>();

        if (request == null) {
            return invalido(resultado, "La prescripcion no puede estar vacia.");
        }

        try {
            validarProfesionalHabilitado(request.getProfesionalId());
        } catch (NoEncontradoExcepcion | IllegalStateException | IllegalArgumentException ex) {
            return invalido(resultado, ex.getMessage());
        }

        Paciente paciente = request.getPacienteId() != null
                ? pacienteRepositorio.findById(request.getPacienteId()).orElse(null)
                : null;
        if (paciente == null) {
            return invalido(resultado, "El paciente indicado no existe.");
        }

        if (request.getMedicamentos() == null || request.getMedicamentos().isEmpty()) {
            return invalido(resultado, "Debe incluir al menos un medicamento en la prescripcion.");
        }

        for (MedicamentoItemDto item : request.getMedicamentos()) {
            if (item == null || item.getDroga() == null || item.getDroga().isBlank()) {
                return invalido(resultado, "Cada medicamento debe indicar su droga o principio activo.");
            }
        }

        if (request.getEvolucionId() != null) {
            EvolucionMedica evolucion = evolucionMedicaRepositorio.findById(request.getEvolucionId()).orElse(null);
            if (evolucion == null) {
                return invalido(resultado, "La evolucion clinica indicada no existe.");
            }
            if (evolucion.getPaciente() != null
                    && !evolucion.getPaciente().getId().equals(paciente.getId())) {
                return invalido(resultado, "La evolucion clinica indicada no pertenece al paciente.");
            }
        }

        resultado.put("valido", true);
        resultado.put("mensaje", "Profesional habilitado y prescripcion valida con formato oficial.");
        return resultado;
    }

    private Map<String, Object> invalido(Map<String, Object> resultado, String mensaje) {
        resultado.put("valido", false);
        resultado.put("mensaje", mensaje != null && !mensaje.isBlank()
                ? mensaje
                : "La prescripcion no cumple los requisitos minimos.");
        return resultado;
    }

    private EvolucionMedica resolverEvolucion(RecetaDigitalDtoRequest request,
                                              Paciente paciente,
                                              Profesional profesional) {
        if (request.getEvolucionId() == null) {
            return null;
        }

        EvolucionMedica evolucion = evolucionMedicaRepositorio.findById(request.getEvolucionId())
                .orElseThrow(() -> new NoEncontradoExcepcion(
                        "Evolucion medica no encontrada con ID: " + request.getEvolucionId()));

        if (evolucion.getPaciente() != null
                && !evolucion.getPaciente().getId().equals(paciente.getId())) {
            throw new IllegalArgumentException("La evolucion indicada no pertenece al paciente de la receta.");
        }
        if (evolucion.getProfesional() != null
                && !evolucion.getProfesional().getId().equals(profesional.getId())) {
            throw new IllegalArgumentException("La evolucion indicada no pertenece al profesional de la receta.");
        }

        return evolucion;
    }

    public void validarProfesionalHabilitado(Long profesionalId) {
        if (profesionalId == null) {
            throw new IllegalArgumentException("El ID del profesional es obligatorio.");
        }

        Profesional profesional = profesionalRepositorio.findById(profesionalId)
            .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + profesionalId));

        if (Boolean.FALSE.equals(profesional.getEstadoActivo())) {
            throw new IllegalStateException("El profesional se encuentra inactivo y no tiene matrícula habilitada para emitir recetas.");
        }

        if (profesional.getMatricula() == null || profesional.getMatricula().trim().isEmpty()) {
            throw new IllegalStateException("El profesional no registra matrícula médica válida para emitir prescripciones oficiales.");
        }
    }

    public RecetaDigital obtenerPorId(Long id) {
        return recetaRepositorio.findById(id)
            .orElseThrow(() -> new NoEncontradoExcepcion("Receta digital no encontrada con ID: " + id));
    }

    public byte[] generarPdf(Long id) {
        RecetaDigital receta = obtenerPorId(id);
        String qrUrl = receta.getCodigoQrUrl() != null ? receta.getCodigoQrUrl() : (frontendUrl + "/verificar-receta?hash=" + receta.getHashIntegridad());
        return pdfServicio.generarRecetaPdf(receta, qrUrl);
    }

    public Map<String, Object> verificarDocumentoPublico(String hash) {
        Map<String, Object> res = new HashMap<>();
        var optReceta = recetaRepositorio.findByHashIntegridad(hash);
        if (optReceta.isPresent()) {
            RecetaDigital r = optReceta.get();
            res.put("valido", true);
            res.put("tipoDocumento", "RECETA_DIGITAL");
            res.put("emisor", "Dr./Dra. " + r.getProfesional().getNombre() + " " + r.getProfesional().getApellido());
            res.put("matricula", r.getProfesional().getMatricula());
            res.put("fechaEmision", r.getFechaEmision());
            res.put("fechaVencimiento", r.getFechaVencimiento());
            res.put("estado", r.getFechaVencimiento().isBefore(LocalDate.now()) ? "VENCIDA" : "VIGENTE");
            res.put("hashSha256", hash);
            return res;
        }

        res.put("valido", false);
        res.put("mensaje", "No se encontró ningún documento oficial registrado con dicho código de verificación.");
        return res;
    }
}
