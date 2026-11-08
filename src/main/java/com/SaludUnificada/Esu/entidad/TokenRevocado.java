package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_revocado", indexes = {
        @Index(name = "idx_token_revocado_jti", columnList = "jti"),
        @Index(name = "idx_token_revocado_expiracion", columnList = "fecha_expiracion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRevocado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jti", nullable = false, unique = true, length = 64)
    private String jti;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_revocacion", nullable = false)
    private LocalDateTime fechaRevocacion;

    @Column(name = "razon", nullable = false, length = 64)
    private String razon;
}
