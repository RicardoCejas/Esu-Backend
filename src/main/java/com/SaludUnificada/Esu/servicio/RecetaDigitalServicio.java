package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.DispensarRecetaDtoRequest;
import com.SaludUnificada.Esu.dto.request.MedicamentoItemDto;
import com.SaludUnificada.Esu.dto.request.RecetaDigitalDtoRequest;
import com.SaludUnificada.Esu.dto.response.DispensarRecetaDtoResponse;
import com.SaludUnificada.Esu.entidad.MedicamentoReceta;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.RecetaVencidaException;
import com.SaludUnificada.Esu.error.RecetaYaDispensadaException;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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
    private CatalogoFarmacologicoServicio catalogoFarmacologicoServicio;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public RecetaDigital emitirReceta(RecetaDigitalDtoRequest request) {
        validarProfesionalHabilitado(request.getProfesionalId());

        Paciente paciente = pacienteRepositorio.findById(request.getPacienteId())
            .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + request.getPacienteId()));

        Profesional profesional = profesionalRepositorio.findById(request.getProfesionalId())
            .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + request.getProfesionalId()));

        // Validación HU-106: No permitir duplicación del mismo principio activo dentro de la misma receta
        if (request.getMedicamentos() != null) {
            Set<String> principiosActivosVistos = new HashSet<>();
            for (MedicamentoItemDto item : request.getMedicamentos()) {
                if (item.getDroga() == null || item.getDroga().trim().isEmpty()) {
                    throw new IllegalArgumentException("Cada renglón debe contener un principio activo o monodroga obligatorio según Ley 27.553");
                }
                String normalizada = catalogoFarmacologicoServicio.normalizar(item.getDroga());
                if (!principiosActivosVistos.add(normalizada)) {
                    throw new IllegalArgumentException(
                        "No se permite prescribir medicamentos duplicados con el mismo principio activo en la misma receta: '" + item.getDroga() + "'"
                    );
                }
            }
        }

        RecetaDigital receta = new RecetaDigital();
        receta.setPaciente(paciente);
        receta.setProfesional(profesional);
        receta.setFechaEmision(LocalDate.now());
        receta.setFechaVencimiento(LocalDate.now().plusDays(30));
        receta.setEstadoReceta("PENDIENTE");
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
                m.setCantidad(item.getCantidad() != null ? String.valueOf(item.getCantidad()) : "1");
                meds.add(m);
            }
        }
        receta.setMedicamentos(meds);

        // Generar hash SHA-256 de integridad antes de persistir
        String textoParaHash = String.format("RECETA|%s|%s|%s|%s|%s|%s",
            paciente.getDni(),
            profesional.getMatricula(),
            receta.getFechaEmision(),
            receta.getDiagnostico(),
            request.getMedicamentos() != null ? request.getMedicamentos().size() : 0,
            System.nanoTime()
        );
        String hash = pdfServicio.calcularHashSha256(textoParaHash);
        receta.setHashIntegridad(hash);

        String qrUrl = frontendUrl + "/farmacia/validar-receta?hash=" + hash;
        receta.setCodigoQrUrl(qrUrl);

        return recetaRepositorio.save(receta);
    }

    /**
     * HU-105: Endpoint transaccional de dispensación farmacéutica
     */
    @Transactional
    public DispensarRecetaDtoResponse dispensarReceta(DispensarRecetaDtoRequest request) {
        String claveBusqueda = extraerHashOId(request.getCodigoOQr());

        RecetaDigital receta = buscarRecetaPorCodigoOHash(claveBusqueda)
            .orElseThrow(() -> new NoEncontradoExcepcion("No se encontró ninguna receta médica registrada con el identificador o QR: " + request.getCodigoOQr()));

        // Validación de dispensación previa (HTTP 409)
        if ("DISPENSADA".equalsIgnoreCase(receta.getEstadoReceta()) || receta.getFechaDispensacion() != null) {
            throw new RecetaYaDispensadaException(
                String.format("La receta oficial #%d (Hash: %s) ya fue DISPENSADA el %s por la farmacia matrícula %s.",
                    receta.getId(),
                    receta.getHashIntegridad(),
                    receta.getFechaDispensacion(),
                    receta.getMatriculaFarmaciaDispensadora() != null ? receta.getMatriculaFarmaciaDispensadora() : "-"
                )
            );
        }

        // Validación de vigencia (HTTP 410)
        LocalDate hoy = LocalDate.now();
        if (receta.getFechaVencimiento() != null && receta.getFechaVencimiento().isBefore(hoy)) {
            receta.setEstadoReceta("VENCIDA");
            recetaRepositorio.save(receta);
            throw new RecetaVencidaException(
                String.format("La receta oficial #%d venció el %s superando los 30 días corridos de vigencia legal reglamentaria. No puede dispensarse.",
                    receta.getId(),
                    receta.getFechaVencimiento()
                )
            );
        }

        // Actualización transaccional
        receta.setEstadoReceta("DISPENSADA");
        receta.setFechaDispensacion(LocalDateTime.now());
        receta.setMatriculaFarmaciaDispensadora(request.getMatriculaFarmacia());

        RecetaDigital actualizada = recetaRepositorio.save(receta);

        return DispensarRecetaDtoResponse.builder()
            .recetaId(actualizada.getId())
            .estadoReceta(actualizada.getEstadoReceta())
            .fechaDispensacion(actualizada.getFechaDispensacion())
            .matriculaFarmacia(actualizada.getMatriculaFarmaciaDispensadora())
            .pacienteNombre(actualizada.getPaciente().getNombre() + " " + actualizada.getPaciente().getApellido())
            .pacienteDni(actualizada.getPaciente().getDni())
            .profesionalNombre("Dr./Dra. " + actualizada.getProfesional().getNombre() + " " + actualizada.getProfesional().getApellido())
            .matriculaProfesional(actualizada.getProfesional().getMatricula())
            .hashIntegridad(actualizada.getHashIntegridad())
            .mensaje("Receta validada y dispensada exitosamente. Se ha registrado el bloqueo para evitar redispesaciones.")
            .build();
    }

    public Optional<RecetaDigital> buscarRecetaPorCodigoOHash(String clave) {
        if (clave == null || clave.trim().isEmpty()) {
            return Optional.empty();
        }
        String limpia = clave.trim();

        // Si es numérico directo, buscar por ID
        if (limpia.matches("^\\d+$")) {
            Optional<RecetaDigital> porId = recetaRepositorio.findById(Long.parseLong(limpia));
            if (porId.isPresent()) return porId;
        }

        return recetaRepositorio.findByHashIntegridad(limpia);
    }

    private String extraerHashOId(String codigoOQr) {
        if (codigoOQr == null) return "";
        String s = codigoOQr.trim();
        if (s.contains("hash=")) {
            int idx = s.indexOf("hash=") + 5;
            int amp = s.indexOf("&", idx);
            return amp != -1 ? s.substring(idx, amp) : s.substring(idx);
        }
        return s;
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
        String qrUrl = receta.getCodigoQrUrl() != null ? receta.getCodigoQrUrl() : (frontendUrl + "/farmacia/validar-receta?hash=" + receta.getHashIntegridad());
        return pdfServicio.generarRecetaPdf(receta, qrUrl);
    }

    public Map<String, Object> verificarDocumentoPublico(String hash) {
        Map<String, Object> res = new HashMap<>();
        String hashLimpio = extraerHashOId(hash);
        var optReceta = buscarRecetaPorCodigoOHash(hashLimpio);
        if (optReceta.isPresent()) {
            RecetaDigital r = optReceta.get();
            boolean vencida = r.getFechaVencimiento() != null && r.getFechaVencimiento().isBefore(LocalDate.now());
            String estado = "DISPENSADA".equalsIgnoreCase(r.getEstadoReceta()) ? "DISPENSADA" : (vencida ? "VENCIDA" : "VIGENTE");

            res.put("valido", true);
            res.put("id", r.getId());
            res.put("tipoDocumento", "RECETA_DIGITAL");
            res.put("emisor", "Dr./Dra. " + r.getProfesional().getNombre() + " " + r.getProfesional().getApellido());
            res.put("matricula", r.getProfesional().getMatricula());
            res.put("paciente", r.getPaciente().getNombre() + " " + r.getPaciente().getApellido());
            res.put("dni", r.getPaciente().getDni());
            res.put("fechaEmision", r.getFechaEmision());
            res.put("fechaVencimiento", r.getFechaVencimiento());
            res.put("fechaDispensacion", r.getFechaDispensacion());
            res.put("farmaciaDispensadora", r.getMatriculaFarmaciaDispensadora());
            res.put("estado", estado);
            res.put("medicamentos", r.getMedicamentos());
            res.put("hashSha256", r.getHashIntegridad());
            return res;
        }

        res.put("valido", false);
        res.put("mensaje", "No se encontró ningún documento oficial registrado con dicho código de verificación.");
        return res;
    }
}
