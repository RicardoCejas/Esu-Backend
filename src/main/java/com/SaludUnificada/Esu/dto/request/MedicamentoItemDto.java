package com.SaludUnificada.Esu.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@NoArgsConstructor
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
    private Integer cantidad = 1;

    private String textoCantidad;

    public MedicamentoItemDto(String nombreComercial, String droga, String presentacion, String dosisIndicada,
                              String posologia, String viaAdministracion, String duracion, Integer cantidad) {
        this.nombreComercial = nombreComercial;
        this.droga = droga;
        this.presentacion = presentacion;
        this.dosisIndicada = dosisIndicada;
        this.posologia = posologia;
        this.viaAdministracion = viaAdministracion;
        this.duracion = duracion;
        this.cantidad = cantidad != null ? cantidad : 1;
        this.textoCantidad = this.cantidad != null ? String.valueOf(this.cantidad) : "1";
    }

    @JsonSetter("cantidad")
    public void setCantidadFromJson(Object raw) {
        if (raw == null) {
            this.cantidad = 1;
            this.textoCantidad = "1";
        } else if (raw instanceof Number) {
            this.cantidad = Math.max(1, ((Number) raw).intValue());
            this.textoCantidad = String.valueOf(this.cantidad);
        } else {
            String str = raw.toString().trim();
            this.textoCantidad = str;
            Matcher m = Pattern.compile("\\d+").matcher(str);
            if (m.find()) {
                try {
                    this.cantidad = Math.max(1, Integer.parseInt(m.group()));
                } catch (Exception ignored) {
                    this.cantidad = 1;
                }
            } else {
                this.cantidad = 1;
            }
        }
    }
}
