package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.response.EvolucionAdendaDtoResponse;
import com.SaludUnificada.Esu.dto.response.EvolucionMedicaDtoResponse;
import com.SaludUnificada.Esu.dto.response.MedicamentoRecetaDtoResponse;
import com.SaludUnificada.Esu.dto.response.RecetaDigitalDtoResponse;
import com.SaludUnificada.Esu.entidad.EvolucionAdenda;
import com.SaludUnificada.Esu.entidad.EvolucionMedica;
import com.SaludUnificada.Esu.entidad.MedicamentoReceta;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Issue #118: conversión a DTOs de las respuestas clínicas para no exponer entidades JPA
 * (ni datos sensibles como el password del usuario asociado).
 */
@Component
public class ClinicaMapper {

    private final PacienteMapper pacienteMapper;
    private final ProfesionalMapper profesionalMapper;

    public ClinicaMapper(PacienteMapper pacienteMapper, ProfesionalMapper profesionalMapper) {
        this.pacienteMapper = pacienteMapper;
        this.profesionalMapper = profesionalMapper;
    }

    public MedicamentoRecetaDtoResponse paraDto(MedicamentoReceta medicamento) {
        if (medicamento == null) {
            return null;
        }
        MedicamentoRecetaDtoResponse dto = new MedicamentoRecetaDtoResponse();
        dto.setId(medicamento.getId());
        dto.setNombreComercial(medicamento.getNombreComercial());
        dto.setDroga(medicamento.getDroga());
        dto.setPresentacion(medicamento.getPresentacion());
        dto.setDosisIndicada(medicamento.getDosisIndicada());
        dto.setPosologia(medicamento.getPosologia());
        dto.setViaAdministracion(medicamento.getViaAdministracion());
        dto.setDuracion(medicamento.getDuracion());
        dto.setCantidad(medicamento.getCantidad());
        return dto;
    }

    public EvolucionAdendaDtoResponse paraDto(EvolucionAdenda adenda) {
        if (adenda == null) {
            return null;
        }
        EvolucionAdendaDtoResponse dto = new EvolucionAdendaDtoResponse();
        dto.setId(adenda.getId());
        dto.setEvolucionId(adenda.getEvolucionMedica() != null ? adenda.getEvolucionMedica().getId() : null);
        dto.setProfesional(profesionalMapper.paraDto(adenda.getProfesional()));
        dto.setJustificacionClinica(adenda.getJustificacionClinica());
        dto.setContenidoAdicional(adenda.getContenidoAdicional());
        dto.setFechaFirma(adenda.getFechaFirma());
        dto.setHashIntegridad(adenda.getHashIntegridad());
        return dto;
    }

    public RecetaDigitalDtoResponse paraDto(RecetaDigital receta) {
        if (receta == null) {
            return null;
        }
        RecetaDigitalDtoResponse dto = new RecetaDigitalDtoResponse();
        dto.setId(receta.getId());
        dto.setPaciente(pacienteMapper.paraDto(receta.getPaciente()));
        dto.setProfesional(profesionalMapper.paraDto(receta.getProfesional()));
        dto.setEvolucionId(receta.getEvolucionMedica() != null ? receta.getEvolucionMedica().getId() : null);
        dto.setFechaEmision(receta.getFechaEmision());
        dto.setFechaVencimiento(receta.getFechaVencimiento());
        dto.setDiagnostico(receta.getDiagnostico());
        dto.setCodigoCie10(receta.getCodigoCie10());
        dto.setIndicacionesGenerales(receta.getIndicacionesGenerales());
        dto.setHashIntegridad(receta.getHashIntegridad());
        dto.setCodigoQrUrl(receta.getCodigoQrUrl());
        dto.setFechaCreacion(receta.getFechaCreacion());
        dto.setMedicamentos(mapearMedicamentos(receta.getMedicamentos()));
        return dto;
    }

    public EvolucionMedicaDtoResponse paraDto(EvolucionMedica evolucion) {
        if (evolucion == null) {
            return null;
        }
        EvolucionMedicaDtoResponse dto = new EvolucionMedicaDtoResponse();
        dto.setId(evolucion.getId());
        dto.setPaciente(pacienteMapper.paraDto(evolucion.getPaciente()));
        dto.setProfesional(profesionalMapper.paraDto(evolucion.getProfesional()));
        dto.setTurnoId(evolucion.getTurno() != null ? evolucion.getTurno().getId() : null);
        dto.setHistoriaClinicaId(evolucion.getHistoriaClinica() != null
                ? evolucion.getHistoriaClinica().getId() : null);
        dto.setMotivoConsulta(evolucion.getMotivoConsulta());
        dto.setAnamnesis(evolucion.getAnamnesis());
        dto.setExamenFisico(evolucion.getExamenFisico());
        dto.setDiagnostico(evolucion.getDiagnostico());
        dto.setCodigoCie10(evolucion.getCodigoCie10());
        dto.setDescripcionCie10(evolucion.getDescripcionCie10());
        dto.setTratamiento(evolucion.getTratamiento());
        dto.setPresionArterial(evolucion.getPresionArterial());
        dto.setFrecuenciaCardiaca(evolucion.getFrecuenciaCardiaca());
        dto.setTemperatura(evolucion.getTemperatura());
        dto.setPeso(evolucion.getPeso());
        dto.setTalla(evolucion.getTalla());
        dto.setImc(evolucion.getImc());
        dto.setFirmada(evolucion.getFirmada());
        dto.setFechaFirma(evolucion.getFechaFirma());
        dto.setHashIntegridad(evolucion.getHashIntegridad());
        dto.setFechaCreacion(evolucion.getFechaCreacion());
        dto.setAdendas(mapearAdendas(evolucion.getAdendas()));
        dto.setRecetas(mapearRecetas(evolucion.getRecetas()));
        return dto;
    }

    private List<MedicamentoRecetaDtoResponse> mapearMedicamentos(List<MedicamentoReceta> medicamentos) {
        if (medicamentos == null || medicamentos.isEmpty()) {
            return Collections.emptyList();
        }
        return medicamentos.stream().map(this::paraDto).toList();
    }

    private List<EvolucionAdendaDtoResponse> mapearAdendas(List<EvolucionAdenda> adendas) {
        if (adendas == null || adendas.isEmpty()) {
            return Collections.emptyList();
        }
        return adendas.stream().map(this::paraDto).toList();
    }

    private List<RecetaDigitalDtoResponse> mapearRecetas(List<RecetaDigital> recetas) {
        if (recetas == null || recetas.isEmpty()) {
            return Collections.emptyList();
        }
        return recetas.stream().map(this::paraDto).toList();
    }
}
