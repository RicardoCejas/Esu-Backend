package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_acceso_hc", indexes = {
    @Index(name = "idx_log_paciente", columnList = "paciente_id"),
    @Index(name = "idx_log_usuario", columnList = "usuario_id"),
    @Index(name = "idx_log_fecha", columnList = "fecha_hora")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogAccesoHc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "rol_usuario", length = 50)
    private String rolUsuario;

    public LogAccesoHc(Long usuarioId, Long pacienteId, String accion, String ipAddress, String rolUsuario) {
        this.usuarioId = usuarioId;
        this.pacienteId = pacienteId;
        this.fechaHora = LocalDateTime.now();
        this.accion = accion;
        this.ipAddress = ipAddress;
        this.rolUsuario = rolUsuario;
    }
}
