package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Issue #120: borrador de consulta. Se exige paciente y profesional para que el
 * autoguardado quede trazado y no sea un simple eco del frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaBorradorDtoRequest {

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "El ID del profesional es obligatorio")
    private Long profesionalId;

    private Long turnoId;

    @NotEmpty(message = "El contenido del borrador no puede estar vacio")
    private Map<String, Object> contenido;
}
