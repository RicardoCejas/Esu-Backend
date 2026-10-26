package com.SaludUnificada.Esu.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentoItemDto {
    private String nombreComercial;

    @NotBlank(message = "La droga o principio activo genérico es obligatorio según Ley 27.553")
    private String droga;

    @NotBlank(message = "La presentación farmacéutica es obligatoria (ej: Comprimidos, Jarabe, etc.)")
    private String presentacion;

    @NotBlank(message = "La dosis indicada es obligatoria")
    private String dosisIndicada;

    private String posologia;
    private String viaAdministracion;

    @NotBlank(message = "La duración del tratamiento es obligatoria (ej: 7 días)")
    private String duracion;

    @NotNull(message = "La cantidad a dispensar es obligatoria")
    @Min(value = 1, message = "La cantidad mínima a prescribir debe ser al menos 1 unidad/envase")
    private Integer cantidad;
}
