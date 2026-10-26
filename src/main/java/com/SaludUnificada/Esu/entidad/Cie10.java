package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cie10", indexes = {
    @Index(name = "idx_cie10_codigo", columnList = "codigo"),
    @Index(name = "idx_cie10_descripcion", columnList = "descripcion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cie10 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(length = 200)
    private String capitulo;

    public Cie10(String codigo, String descripcion, String capitulo) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.capitulo = capitulo;
    }
}
