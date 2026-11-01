package com.SaludUnificada.Esu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PacienteDtoResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
    private String email;
    private String obraSocial;
    private String nroAfiliado;
    private String grupoSanguineo;
    private LocalDate fechaNacimiento;
    private Long centroSaludReferenciaId;
    private String centroSaludReferenciaNombre;

    public PacienteDtoResponse(Long id, String nombre, String apellido, String dni, String telefono, String email,
                               Long centroSaludReferenciaId, String centroSaludReferenciaNombre) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
        this.telefono = telefono;
        this.email = email;
        this.centroSaludReferenciaId = centroSaludReferenciaId;
        this.centroSaludReferenciaNombre = centroSaludReferenciaNombre;
    }
}
