package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecetaDigitalDtoRequest {
    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El ID del profesional es obligatorio")
    private Long profesionalId;

    private Long evolucionId;
    private String diagnostico;
    private String codigoCie10;
    private String indicacionesGenerales;
    private Long centroSaludEmisorId;
    private String centroSaludEmisorNombre;
    private String lugarRetiroSugerido;

    @NotEmpty(message = "Debe incluir al menos un medicamento en la prescripción")
    private List<@Valid MedicamentoItemDto> medicamentos;
}
