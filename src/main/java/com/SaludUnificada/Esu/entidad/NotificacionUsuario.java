package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion_usuario", indexes = {
    @Index(name = "idx_notif_usuario_fecha", columnList = "usuario_id, fecha_creacion"),
    @Index(name = "idx_notif_usuario_leida", columnList = "usuario_id, leida")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensaje;

    @Column(nullable = false, length = 50)
    private String tipo; // TURNO, RECETA, RECORDATORIO, SISTEMA

    @Column(nullable = false)
    private Boolean leida = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "link_accion", length = 255)
    private String linkAccion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (leida == null) {
            leida = false;
        }
    }
}
