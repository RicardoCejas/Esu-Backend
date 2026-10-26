package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "consentimientos_informados", indexes = {
        @Index(name = "idx_consentimiento_paciente", columnList = "paciente_id"),
        @Index(name = "idx_consentimiento_profesional", columnList = "profesional_id"),
        @Index(name = "idx_consentimiento_hash", columnList = "hash_integridad_sha256")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsentimientoInformado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(nullable = false, length = 200)
    private String procedimiento;

    @Lob
    @Column(name = "texto_legal", nullable = false, columnDefinition = "LONGTEXT")
    private String textoLegal;

    @Lob
    @Column(name = "firma_digital_base64", nullable = false, columnDefinition = "LONGTEXT")
    private String firmaDigitalBase64;

    @Column(name = "ip_firma", nullable = false, length = 45)
    private String ipFirma;

    @Column(name = "hash_integridad_sha256", nullable = false, length = 64)
    private String hashIntegridadSha256;

    @Column(name = "fecha_hora_firma", nullable = false, updatable = false)
    private LocalDateTime fechaHoraFirma;

    @Column(nullable = false, length = 30)
    private String estado = "FIRMADO"; // FIRMADO, REVOCADO

    @PrePersist
    public void prePersist() {
        if (this.fechaHoraFirma == null) {
            this.fechaHoraFirma = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = "FIRMADO";
        }
    }

    /**
     * Garantía legal de inmutabilidad (Ley 26.529 Derechos del Paciente).
     * Los registros de consentimiento firmado no pueden ser modificados.
     */
    @PreUpdate
    public void preUpdate() {
        throw new IllegalStateException("Violación de Inmutabilidad Legal (Ley 26.529): Un Consentimiento Informado formalizado no puede ser modificado.");
    }

    /**
     * Garantía legal de inmutabilidad (Ley 26.529 Derechos del Paciente).
     * Los registros de consentimiento firmado no pueden ser eliminados de la HCU.
     */
    @PreRemove
    public void preRemove() {
        throw new IllegalStateException("Violación de Inmutabilidad Legal (Ley 26.529): Un Consentimiento Informado formalizado no puede ser eliminado del expediente clínico.");
    }
}
