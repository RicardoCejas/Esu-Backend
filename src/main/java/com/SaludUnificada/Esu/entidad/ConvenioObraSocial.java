package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "convenio_obra_social")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConvenioObraSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_obra_social", nullable = false)
    private String nombreObraSocial;

    @Column(name = "sigla", nullable = false)
    private String sigla;

    @Column(name = "codigo_rnosc")
    private String codigoRnosc;

    @Column(name = "patron_regex_afiliado")
    private String patronRegexAfiliado;

    @Column(name = "requiere_coseguro")
    private Boolean requiereCoseguro = false;

    @Column(name = "activo")
    private Boolean activo = true;
}
