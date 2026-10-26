package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.AlergiaDtoRequest;
import com.SaludUnificada.Esu.dto.request.MedicamentoItemDto;
import com.SaludUnificada.Esu.dto.response.AlergiaDtoResponse;
import com.SaludUnificada.Esu.entidad.AlergiaPaciente;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.repositorio.AlergiaPacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlergiaServicio {

    private final AlergiaPacienteRepositorio alergiaRepositorio;
    private final PacienteRepositorio pacienteRepositorio;

    @Transactional
    public AlergiaDtoResponse registrarAlergia(Long pacienteId, AlergiaDtoRequest request) {
        if (!pacienteRepositorio.existsById(pacienteId)) {
            throw new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId);
        }

        AlergiaPaciente entidad = new AlergiaPaciente();
        entidad.setPacienteId(pacienteId);
        entidad.setSustanciaAlergeno(request.getSustanciaAlergeno().trim());
        entidad.setSeveridad(request.getSeveridad().toUpperCase().trim());
        entidad.setFechaDiagnostico(request.getFechaDiagnostico());
        entidad.setObservaciones(request.getObservaciones());
        entidad.setActivo(true);

        AlergiaPaciente guardado = alergiaRepositorio.save(entidad);
        return mapearADto(guardado);
    }

    @Transactional(readOnly = true)
    public List<AlergiaDtoResponse> listarAlergiasPaciente(Long pacienteId) {
        if (!pacienteRepositorio.existsById(pacienteId)) {
            throw new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId);
        }
        return alergiaRepositorio.findByPacienteIdAndActivoTrueOrderByFechaRegistroDesc(pacienteId)
                .stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminarAlergia(Long id) {
        AlergiaPaciente entidad = alergiaRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Alergia no encontrada con ID: " + id));
        entidad.setActivo(false);
        alergiaRepositorio.save(entidad);
    }

    /**
     * Interceptor Clínico de Contraindicaciones y Alergias Preexistentes (HU-104)
     * Compara los principios activos y nombres comerciales de los fármacos recetados
     * contra las alergias activas del paciente. Si detecta coincidencia con severidad GRAVE,
     * bloquea la operación lanzando ReglaNegocioException.
     */
    @Transactional(readOnly = true)
    public void validarContraindicaciones(Long pacienteId, List<MedicamentoItemDto> medicamentos) {
        if (medicamentos == null || medicamentos.isEmpty()) {
            return;
        }

        List<AlergiaPaciente> alergiasActivas = alergiaRepositorio.findByPacienteIdAndActivoTrueOrderByFechaRegistroDesc(pacienteId);
        if (alergiasActivas.isEmpty()) {
            return;
        }

        for (AlergiaPaciente alergia : alergiasActivas) {
            String alergenoNormalizado = normalizarTexto(alergia.getSustanciaAlergeno());

            for (MedicamentoItemDto med : medicamentos) {
                String drogaNormalizada = normalizarTexto(med.getDroga());
                String nombreComercialNormalizado = normalizarTexto(med.getNombreComercial());

                boolean coincideDroga = drogaNormalizada != null && (drogaNormalizada.contains(alergenoNormalizado) || alergenoNormalizado.contains(drogaNormalizada));
                boolean coincideNombre = nombreComercialNormalizado != null && (nombreComercialNormalizado.contains(alergenoNormalizado) || alergenoNormalizado.contains(nombreComercialNormalizado));

                if (coincideDroga || coincideNombre) {
                    if ("GRAVE".equalsIgnoreCase(alergia.getSeveridad())) {
                        throw new ReglaNegocioException(String.format(
                                "INTERCEPTOR CLÍNICO BLOQUEANTE: El paciente registra ALERGIA GRAVE a '%s' (riesgo anafiláctico). Fármaco contraindicado: '%s' (%s). Prescripción rechazada por seguridad del paciente.",
                                alergia.getSustanciaAlergeno(),
                                med.getDroga() != null ? med.getDroga() : med.getNombreComercial(),
                                alergia.getObservaciones() != null ? alergia.getObservaciones() : "Sin observaciones adicionales"
                        ));
                    }
                }
            }
        }
    }

    private String normalizarTexto(String input) {
        if (input == null) return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase(Locale.ROOT).trim();
    }

    private AlergiaDtoResponse mapearADto(AlergiaPaciente entidad) {
        return new AlergiaDtoResponse(
                entidad.getId(),
                entidad.getPacienteId(),
                entidad.getSustanciaAlergeno(),
                entidad.getSeveridad(),
                entidad.getFechaDiagnostico(),
                entidad.getObservaciones(),
                entidad.getActivo(),
                entidad.getFechaRegistro()
        );
    }
}
