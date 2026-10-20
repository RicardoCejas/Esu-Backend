package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinalizarAtencionDtoRequest {
    @NotBlank(message = "El motivo de consulta es obligatorio")
    private String motivoConsulta;

    private String anamnesis;
    private String examenFisico;

    @NotBlank(message = "El diagnóstico es obligatorio")
    private String diagnostico;

    private String codigoCie10;
    private String descripcionCie10;
    private String tratamiento;

    // Signos Vitales
    private String presionArterial;
    private String frecuenciaCardiaca;
    private String temperatura;
    private String peso;
    private String talla;
    private String imc;

    // Receta opcional
    private Boolean incluirReceta = false;
    private List<MedicamentoItemDto> medicamentos;
}
