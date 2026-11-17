-- V14: Auditoría durable de fusiones de pacientes
CREATE TABLE IF NOT EXISTS auditoria_fusion_paciente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_origen_id BIGINT NOT NULL,
    paciente_destino_id BIGINT NOT NULL,
    operador_email VARCHAR(150) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    fecha_hora DATETIME NOT NULL,
    detalles TEXT,
    INDEX idx_fusion_origen (paciente_origen_id),
    INDEX idx_fusion_destino (paciente_destino_id),
    INDEX idx_fusion_fecha (fecha_hora)
);
