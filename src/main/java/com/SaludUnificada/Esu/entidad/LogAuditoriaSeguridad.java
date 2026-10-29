package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_auditoria_seguridad", indexes = {
    @Index(name = "idx_auditoria_email_fecha", columnList = "email, fecha"),
    @Index(name = "idx_auditoria_evento", columnList = "evento"),
    @Index(name = "idx_auditoria_ip", columnList = "ip_address")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogAuditoriaSeguridad {

    public enum EventoSeguridad {
        LOGIN_OK,
        LOGIN_FAIL,
        REENVIO_VERIFICACION,
        SOLICITUD_RECUPERACION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventoSeguridad evento;

    @Column(length = 250)
    private String detalle;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
