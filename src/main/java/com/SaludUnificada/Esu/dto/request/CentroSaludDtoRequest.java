package com.SaludUnificada.Esu.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de alta o actualización de un centro de salud / sede asistencial")
public class CentroSaludDtoRequest {

    @NotBlank(message = "El nombre del centro de salud es obligatorio")
    @Schema(description = "Nombre oficial del establecimiento", example = "Hospital Regional Aurelio Crespo")
    private String nombre;

    @NotBlank(message = "El tipo de establecimiento es obligatorio")
    @Schema(description = "Tipo (HOSPITAL, CLINICA, DISPENSARIO, CONSULTORIO_PRIVADO)", example = "HOSPITAL")
    private String tipo;

    @NotBlank(message = "La dirección física del centro es obligatoria")
    @Schema(description = "Dirección postal del establecimiento", example = "Av. Juan B. Justo 1200, Cruz del Eje")
    private String direccion;

    @Schema(description = "Teléfono de contacto o guardia", example = "03549-422001")
    private String telefono;

    @Schema(description = "Ciudad o localidad", example = "Cruz del Eje")
    private String ciudad;

    @Schema(description = "Descripción de horarios de atención", example = "24 Horas (Guardia permanente)")
    private String horarioAtencion;

    @Schema(description = "Indica si dispone de guardia activa las 24 horas", example = "true")
    private Boolean disponibleGuardia;

    @Schema(description = "Latitud geográfica GPS", example = "-30.7312")
    private Double latitud;

    @Schema(description = "Longitud geográfica GPS", example = "-64.7985")
    private Double longitud;
}
