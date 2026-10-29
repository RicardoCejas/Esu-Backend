package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_verificacion_documento", indexes = {
    @Index(name = "idx_token_verif_doc", columnList = "token"),
    @Index(name = "idx_token_paciente", columnList = "paciente_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenVerificacionDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "tipo_documento", nullable = false, length = 50)
    private String tipoDocumento;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private Boolean usado = false;

    @Column(nullable = false)
    private Boolean revocado = false;

    public TokenVerificacionDocumento(String token, Long pacienteId, String tipoDocumento, LocalDateTime fechaExpiracion) {
        this.token = token;
        this.pacienteId = pacienteId;
        this.tipoDocumento = tipoDocumento;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaExpiracion = fechaExpiracion;
        this.usado = false;
        this.revocado = false;
    }

    public boolean esValido() {
        return !Boolean.TRUE.equals(revocado) &&
               fechaExpiracion != null &&
               fechaExpiracion.isAfter(LocalDateTime.now());
    }
}
