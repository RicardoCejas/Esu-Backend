package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.response.EvolucionAdendaDtoResponse;
import com.SaludUnificada.Esu.dto.response.EvolucionMedicaDtoResponse;
import com.SaludUnificada.Esu.entidad.EvolucionAdenda;
import com.SaludUnificada.Esu.entidad.EvolucionMedica;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EvolucionMedicaMapper {

    public EvolucionMedicaDtoResponse toDto(EvolucionMedica entidad) {
        if (entidad == null) return null;

        List<EvolucionAdendaDtoResponse> adendas = entidad.getAdendas() != null
                ? entidad.getAdendas().stream().map(this::toAdendaDto).collect(Collectors.toList())
                : Collections.emptyList();

        String pacNombre = entidad.getPaciente() != null
                ? entidad.getPaciente().getNombre() + " " + entidad.getPaciente().getApellido()
                : "";

        String profNombre = entidad.getProfesional() != null
                ? "Dr./Dra. " + entidad.getProfesional().getNombre() + " " + entidad.getProfesional().getApellido()
                : "";

        return EvolucionMedicaDtoResponse.builder()
                .id(entidad.getId())
                .pacienteId(entidad.getPaciente() != null ? entidad.getPaciente().getId() : null)
                .pacienteNombre(pacNombre)
                .profesionalId(entidad.getProfesional() != null ? entidad.getProfesional().getId() : null)
                .profesionalNombre(profNombre)
                .turnoId(entidad.getTurno() != null ? entidad.getTurno().getId() : null)
                .historiaClinicaId(entidad.getHistoriaClinica() != null ? entidad.getHistoriaClinica().getId() : null)
                .motivoConsulta(entidad.getMotivoConsulta())
                .anamnesis(entidad.getAnamnesis())
                .examenFisico(entidad.getExamenFisico())
                .diagnostico(entidad.getDiagnostico())
                .codigoCie10(entidad.getCodigoCie10())
                .descripcionCie10(entidad.getDescripcionCie10())
                .tratamiento(entidad.getTratamiento())
                .presionArterial(entidad.getPresionArterial())
                .frecuenciaCardiaca(entidad.getFrecuenciaCardiaca())
                .temperatura(entidad.getTemperatura())
                .peso(entidad.getPeso())
                .talla(entidad.getTalla())
                .imc(entidad.getImc())
                .firmada(entidad.getFirmada())
                .fechaFirma(entidad.getFechaFirma())
                .hashIntegridad(entidad.getHashIntegridad())
                .fechaCreacion(entidad.getFechaCreacion())
                .adendas(adendas)
                .build();
    }

    public EvolucionAdendaDtoResponse toAdendaDto(EvolucionAdenda adenda) {
        if (adenda == null) return null;

        String profNombre = adenda.getProfesional() != null
                ? "Dr./Dra. " + adenda.getProfesional().getNombre() + " " + adenda.getProfesional().getApellido()
                : "";

        return EvolucionAdendaDtoResponse.builder()
                .id(adenda.getId())
                .evolucionId(adenda.getEvolucionMedica() != null ? adenda.getEvolucionMedica().getId() : null)
                .profesionalId(adenda.getProfesional() != null ? adenda.getProfesional().getId() : null)
                .profesionalNombre(profNombre)
                .justificacionClinica(adenda.getJustificacionClinica())
                .contenidoAdicional(adenda.getContenidoAdicional())
                .fechaFirma(adenda.getFechaFirma())
                .hashIntegridad(adenda.getHashIntegridad())
                .build();
    }
}
