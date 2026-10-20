package com.SaludUnificada.Esu.entidad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medicamento_receta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentoReceta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", nullable = false)
    @JsonIgnore
    private RecetaDigital receta;

    @Column(name = "nombre_comercial")
    private String nombreComercial;

    @Column(nullable = false)
    private String droga;

    @Column
    private String presentacion;

    @Column(name = "dosis_indicada")
    private String dosisIndicada;

    @Column
    private String posologia;

    @Column(name = "via_administracion")
    private String viaAdministracion;

    @Column
    private String duracion;

    @Column
    private String cantidad;
}
