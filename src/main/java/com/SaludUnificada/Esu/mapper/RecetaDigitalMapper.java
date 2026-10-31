package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.response.MedicamentoRecetaDtoResponse;
import com.SaludUnificada.Esu.dto.response.RecetaDigitalDtoResponse;
import com.SaludUnificada.Esu.entidad.MedicamentoReceta;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecetaDigitalMapper {

    public RecetaDigitalDtoResponse toDto(RecetaDigital entidad) {
        if (entidad == null) return null;

        List<MedicamentoRecetaDtoResponse> meds = entidad.getMedicamentos() != null
                ? entidad.getMedicamentos().stream().map(this::toMedicamentoDto).collect(Collectors.toList())
                : Collections.emptyList();

        String pacNombre = entidad.getPaciente() != null
                ? entidad.getPaciente().getNombre() + " " + entidad.getPaciente().getApellido()
                : "";
        String pacDni = entidad.getPaciente() != null ? entidad.getPaciente().getDni() : "";

        String profNombre = entidad.getProfesional() != null
                ? "Dr./Dra. " + entidad.getProfesional().getNombre() + " " + entidad.getProfesional().getApellido()
                : "";
        String profMat = entidad.getProfesional() != null ? entidad.getProfesional().getMatricula() : "";

        return RecetaDigitalDtoResponse.builder()
                .id(entidad.getId())
                .pacienteId(entidad.getPaciente() != null ? entidad.getPaciente().getId() : null)
                .pacienteNombre(pacNombre)
                .pacienteDni(pacDni)
                .profesionalId(entidad.getProfesional() != null ? entidad.getProfesional().getId() : null)
                .profesionalNombre(profNombre)
                .profesionalMatricula(profMat)
                .centroSaludEmisorId(entidad.getCentroSaludEmisorId() != null ? entidad.getCentroSaludEmisorId() : 1L)
                .centroSaludEmisorNombre(entidad.getCentroSaludEmisorNombre() != null ? entidad.getCentroSaludEmisorNombre() : "Hospital Regional Aurelio Crespo")
                .farmaciaDispensadoraNombre(entidad.getFarmaciaDispensadoraNombre())
                .lugarRetiroSugerido(entidad.getLugarRetiroSugerido() != null ? entidad.getLugarRetiroSugerido() : "Farmacia Central Hospital Regional Aurelio Crespo o CAPS barrial")
                .fechaEmision(entidad.getFechaEmision())
                .fechaVencimiento(entidad.getFechaVencimiento())
                .diagnostico(entidad.getDiagnostico())
                .codigoCie10(entidad.getCodigoCie10())
                .indicacionesGenerales(entidad.getIndicacionesGenerales())
                .estadoReceta(entidad.getEstadoReceta())
                .fechaDispensacion(entidad.getFechaDispensacion())
                .matriculaFarmaciaDispensadora(entidad.getMatriculaFarmaciaDispensadora())
                .hashIntegridad(entidad.getHashIntegridad())
                .codigoQrUrl(entidad.getCodigoQrUrl())
                .motivoAnulacion(entidad.getMotivoAnulacion())
                .fechaAnulacion(entidad.getFechaAnulacion())
                .medicamentos(meds)
                .build();
    }

    public MedicamentoRecetaDtoResponse toMedicamentoDto(MedicamentoReceta m) {
        if (m == null) return null;
        return MedicamentoRecetaDtoResponse.builder()
                .id(m.getId())
                .nombreComercial(m.getNombreComercial())
                .droga(m.getDroga())
                .presentacion(m.getPresentacion())
                .dosisIndicada(m.getDosisIndicada())
                .posologia(m.getPosologia())
                .viaAdministracion(m.getViaAdministracion())
                .duracion(m.getDuracion())
                .cantidad(m.getCantidad())
                .build();
    }
}
