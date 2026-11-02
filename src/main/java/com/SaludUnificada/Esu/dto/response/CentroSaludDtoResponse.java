package com.SaludUnificada.Esu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle de un centro de salud / sede asistencial")
public class CentroSaludDtoResponse {
    private Long id;
    private String nombre;
    private String tipo;
    private String direccion;
    private String telefono;
    private String ciudad;
    private String horarioAtencion;
    private Boolean disponibleGuardia;
    private Double latitud;
    private Double longitud;
    private Boolean activo;
    private Boolean permiteConsultas;
    private Boolean permiteEstudios;
    private List<String> estudiosDisponibles;
}
