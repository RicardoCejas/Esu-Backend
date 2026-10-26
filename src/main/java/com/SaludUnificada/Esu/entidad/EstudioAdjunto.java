package com.SaludUnificada.Esu.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "estudios_adjuntos", indexes = {
        @Index(name = "idx_estudio_paciente", columnList = "paciente_id"),
        @Index(name = "idx_estudio_evolucion", columnList = "evolucion_id"),
        @Index(name = "idx_estudio_checksum", columnList = "checksum_sha256")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstudioAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "evolucion_id")
    private Long evolucionId;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "nombre_almacenado", nullable = false, length = 255)
    private String nombreAlmacenado;

    @Column(name = "ruta_almacenamiento", nullable = false, length = 500)
    private String rutaAlmacenamiento;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "tamanio_bytes", nullable = false)
    private Long tamanioBytes;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "tipo_estudio", nullable = false, length = 50)
    private String tipoEstudio; // LABORATORIO, IMAGEN, INFORME, RAYOS_X

    @Column(name = "fecha_estudio")
    private LocalDate fechaEstudio;

    @Column(name = "fecha_carga", nullable = false, updatable = false)
    private LocalDateTime fechaCarga;

    @PrePersist
    public void prePersist() {
        if (this.fechaCarga == null) {
            this.fechaCarga = LocalDateTime.now();
        }
        if (this.tipoEstudio == null) {
            this.tipoEstudio = "INFORME";
        }
    }
}
