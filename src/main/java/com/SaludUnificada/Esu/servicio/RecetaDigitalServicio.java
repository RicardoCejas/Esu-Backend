package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.DispensarRecetaDtoRequest;
import com.SaludUnificada.Esu.dto.request.MedicamentoItemDto;
import com.SaludUnificada.Esu.dto.request.RecetaDigitalDtoRequest;
import com.SaludUnificada.Esu.dto.response.DispensarRecetaDtoResponse;
import com.SaludUnificada.Esu.dto.response.VerificacionRecetaPublicaDtoResponse;
import com.SaludUnificada.Esu.entidad.MedicamentoReceta;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.RecetaVencidaException;
import com.SaludUnificada.Esu.error.RecetaYaDispensadaException;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.evento.RecetaEmitidaEvent;
import com.SaludUnificada.Esu.repositorio.EvolucionMedicaRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RecetaDigitalServicio {

    private final RecetaDigitalRepositorio recetaRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final DocumentoPdfServicio pdfServicio;
    private final EmailNotificacionServicio emailServicio;
    private final AlergiaServicio alergiaServicio;
    private final CatalogoFarmacologicoServicio catalogoFarmacologicoServicio;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private EvolucionMedicaRepositorio evolucionRepositorio;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public RecetaDigital emitirReceta(RecetaDigitalDtoRequest request) {
        validarProfesionalHabilitado(request.getProfesionalId());

        if (alergiaServicio != null) {
            alergiaServicio.validarContraindicaciones(request.getPacienteId(), request.getMedicamentos());
        }

        Paciente paciente = pacienteRepositorio.findById(request.getPacienteId())
            .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + request.getPacienteId()));

        Profesional profesional = profesionalRepositorio.findById(request.getProfesionalId())
            .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado con ID: " + request.getProfesionalId()));

        if (request.getEvolucionId() != null && evolucionRepositorio != null) {
            var evOpt = evolucionRepositorio.findById(request.getEvolucionId());
            if (evOpt.isPresent()) {
                var ev = evOpt.get();
                if (ev.getPaciente() != null && !ev.getPaciente().getId().equals(paciente.getId())) {
                    throw new ReglaNegocioException("La evolución médica especificada no corresponde al paciente de la receta.");
                }
                if (ev.getProfesional() != null && !ev.getProfesional().getId().equals(profesional.getId())) {
                    throw new ReglaNegocioException("La evolución médica especificada no corresponde al profesional prescriptor.");
                }
            }
        }

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

        String hash = calcularHashReceta(receta);
        receta.setHashIntegridad(hash);

        String qrUrl = frontendUrl + "/farmacia/validar-receta?hash=" + hash;
        receta.setCodigoQrUrl(qrUrl);

        RecetaDigital guardada = recetaRepositorio.save(receta);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new RecetaEmitidaEvent(guardada.getId(), qrUrl));
        }

        return guardada;
    }

    public String calcularHashReceta(RecetaDigital receta) {
        StringBuilder canonico = new StringBuilder();
        canonico.append("RECETA|")
                .append(receta.getPaciente() != null && receta.getPaciente().getDni() != null ? receta.getPaciente().getDni().trim() : "").append("|")
                .append(receta.getProfesional() != null && receta.getProfesional().getMatricula() != null ? receta.getProfesional().getMatricula().trim() : "").append("|")
                .append(receta.getFechaEmision()).append("|")
                .append(receta.getFechaVencimiento()).append("|")
                .append(receta.getCodigoCie10() != null ? receta.getCodigoCie10().trim() : "").append("|")
                .append(receta.getDiagnostico() != null ? receta.getDiagnostico().trim() : "").append("|")
                .append(receta.getIndicacionesGenerales() != null ? receta.getIndicacionesGenerales().trim() : "");

        if (receta.getMedicamentos() != null) {
            for (MedicamentoReceta item : receta.getMedicamentos()) {
                canonico.append("|MED:")
                        .append(item.getDroga() != null ? item.getDroga().trim() : "").append(";")
                        .append(item.getNombreComercial() != null ? item.getNombreComercial().trim() : "").append(";")
                        .append(item.getPresentacion() != null ? item.getPresentacion().trim() : "").append(";")
                        .append(item.getDosisIndicada() != null ? item.getDosisIndicada().trim() : "").append(";")
                        .append(item.getPosologia() != null ? item.getPosologia().trim() : "").append(";")
                        .append(item.getViaAdministracion() != null ? item.getViaAdministracion().trim() : "").append(";")
                        .append(item.getDuracion() != null ? item.getDuracion().trim() : "").append(";")
                        .append(item.getCantidad() != null ? item.getCantidad() : "1");
            }
        }
        return pdfServicio.calcularHashSha256(canonico.toString());
    }

    @Transactional
    public DispensarRecetaDtoResponse dispensarReceta(DispensarRecetaDtoRequest request) {
        String claveBusqueda = extraerHashOId(request.getCodigoOQr());

        RecetaDigital receta = buscarRecetaPorCodigoOHash(claveBusqueda)
            .orElseThrow(() -> new NoEncontradoExcepcion("No se encontró ninguna receta médica registrada con el identificador o QR: " + request.getCodigoOQr()));

        if ("ANULADA".equalsIgnoreCase(receta.getEstadoReceta())) {
            throw new ReglaNegocioException(String.format("La receta oficial #%d fue ANULADA y no puede ser dispensada.", receta.getId()));
        }
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

        if (limpia.matches("^\\d+$")) {
            Long id = Long.parseLong(limpia);
            Optional<RecetaDigital> porId = recetaRepositorio.findByIdForUpdate(id);
            if (porId.isEmpty()) {
                porId = recetaRepositorio.findById(id);
            }
            if (porId.isPresent()) return porId;
        }

        Optional<RecetaDigital> porHash = recetaRepositorio.findByHashIntegridadForUpdate(limpia);
        if (porHash.isEmpty()) {
            porHash = recetaRepositorio.findByHashIntegridad(limpia);
        }
        return porHash;
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

    @Transactional(readOnly = true)
    public Optional<RecetaDigital> buscarRecetaPorCodigoOHashReadOnly(String clave) {
        if (clave == null || clave.trim().isEmpty()) {
            return Optional.empty();
        }
        String limpia = clave.trim();

        if (limpia.matches("^\\d+$")) {
            Long id = Long.parseLong(limpia);
            Optional<RecetaDigital> porId = recetaRepositorio.findById(id);
            if (porId.isPresent()) return porId;
        }

        return recetaRepositorio.findByHashIntegridad(limpia);
    }

    @Transactional(readOnly = true)
    public VerificacionRecetaPublicaDtoResponse verificarDocumentoPublico(String hash) {
        String hashLimpio = extraerHashOId(hash);
        var optReceta = buscarRecetaPorCodigoOHashReadOnly(hashLimpio);
        if (optReceta.isPresent()) {
            RecetaDigital r = optReceta.get();
            boolean vencida = r.getFechaVencimiento() != null && r.getFechaVencimiento().isBefore(LocalDate.now());
            String estado = "DISPENSADA".equalsIgnoreCase(r.getEstadoReceta()) ? "DISPENSADA" :
                    ("ANULADA".equalsIgnoreCase(r.getEstadoReceta()) ? "ANULADA" : (vencida ? "VENCIDA" : "VIGENTE"));

            String hashCalculado = calcularHashReceta(r);
            boolean integridadValida = hashCalculado != null && hashCalculado.equalsIgnoreCase(r.getHashIntegridad());

            if (!integridadValida) {
                return VerificacionRecetaPublicaDtoResponse.builder()
                    .valido(false)
                    .integridadValida(false)
                    .id(r.getId())
                    .tipoDocumento("RECETA_DIGITAL")
                    .estado("INTEGRIDAD_INVALIDA")
                    .hashIntegridadSha256(r.getHashIntegridad())
                    .mensaje("Falla en la verificación de integridad: el documento ha sido alterado o la firma digital no coincide.")
                    .build();
            }

            return VerificacionRecetaPublicaDtoResponse.builder()
                .valido(true)
                .id(r.getId())
                .tipoDocumento("RECETA_DIGITAL")
                .estado(estado)
                .fechaEmision(r.getFechaEmision())
                .fechaVencimiento(r.getFechaVencimiento())
                .fechaDispensacion(r.getFechaDispensacion())
                .farmaciaDispensadora(r.getMatriculaFarmaciaDispensadora())
                .matriculaProfesional(r.getProfesional() != null ? r.getProfesional().getMatricula() : null)
                .hashIntegridadSha256(r.getHashIntegridad())
                .integridadValida(true)
                .mensaje("Documento oficial validado. Cumple con la normativa de firma e integridad electrónica.")
                .build();
        }

        return VerificacionRecetaPublicaDtoResponse.builder()
            .valido(false)
            .integridadValida(false)
            .mensaje("No se encontró ningún documento oficial registrado con dicho código de verificación.")
            .build();
    }

    @Transactional
    public RecetaDigital anularReceta(Long id, String motivo, String emailProfesional) {
        RecetaDigital receta = obtenerPorId(id);
        if ("DISPENSADA".equalsIgnoreCase(receta.getEstadoReceta())) {
            throw new ReglaNegocioException("No es posible anular una receta medica que ya ha sido dispensada en farmacia.");
        }
        if ("ANULADA".equalsIgnoreCase(receta.getEstadoReceta())) {
            throw new ReglaNegocioException("La receta medica ya se encuentra anulada.");
        }
        receta.setEstadoReceta("ANULADA");
        receta.setMotivoAnulacion(motivo != null && !motivo.isBlank() ? motivo.trim() : "Anulada por prescripcion medica");
        receta.setFechaAnulacion(LocalDateTime.now());
        return recetaRepositorio.save(receta);
    }

    @Transactional(readOnly = true)
    public List<RecetaDigital> listarPorPaciente(Long pacienteId, String estado) {
        List<RecetaDigital> lista = recetaRepositorio.findByPacienteIdOrderByFechaEmisionDesc(pacienteId);
        if (estado != null && !estado.isBlank()) {
            return lista.stream()
                    .filter(r -> estado.equalsIgnoreCase(r.getEstadoReceta()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return lista;
    }

    @Transactional
    public int marcarRecetasVencidas() {
        List<RecetaDigital> pendientesVencidas = recetaRepositorio.findByEstadoRecetaAndFechaVencimientoLessThan("PENDIENTE", LocalDate.now());
        for (RecetaDigital r : pendientesVencidas) {
            r.setEstadoReceta("VENCIDA");
        }
        if (!pendientesVencidas.isEmpty()) {
            recetaRepositorio.saveAll(pendientesVencidas);
        }
        return pendientesVencidas.size();
    }

    @Transactional(readOnly = true)
    public List<RecetaDigital> listarPorCodigoCie10(String cie10) {
        if (cie10 == null || cie10.isBlank()) {
            return List.of();
        }
        return recetaRepositorio.findByCodigoCie10(cie10.trim().toUpperCase());
    }
}
