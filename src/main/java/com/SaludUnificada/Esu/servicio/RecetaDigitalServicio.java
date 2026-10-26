package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.MedicamentoItemDto;
import com.SaludUnificada.Esu.dto.request.RecetaDigitalDtoRequest;
import com.SaludUnificada.Esu.entidad.MedicamentoReceta;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
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
    private DocumentoPdfServicio pdfServicio;

    @Autowired
    private AlergiaServicio alergiaServicio;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public RecetaDigital emitirReceta(RecetaDigitalDtoRequest request) {
        validarProfesionalHabilitado(request.getProfesionalId());

        // HU-104: Interceptor Clínico de Contraindicaciones y Alergias Preexistentes
        alergiaServicio.validarContraindicaciones(request.getPacienteId(), request.getMedicamentos());

        Paciente paciente = pacienteRepositorio.findById(request.getPacienteId())
            .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + request.getPacienteId()));

        Profesional profesional = profesionalRepositorio.findById(request.getProfesionalId())
            .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + request.getProfesionalId()));

        RecetaDigital receta = new RecetaDigital();
        receta.setPaciente(paciente);
        receta.setProfesional(profesional);
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

    public void validarProfesionalHabilitado(Long profesionalId) {
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
