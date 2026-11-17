-- Migración V13: Tabla para persistencia real de borradores clínicos en consultas médicas
CREATE TABLE IF NOT EXISTS consulta_borrador (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    profesional_id BIGINT NOT NULL,
    paciente_id BIGINT,
    turno_id BIGINT,
    motivo_consulta VARCHAR(255),
    anamnesis TEXT,
    examen_fisico TEXT,
    diagnostico VARCHAR(255),
    codigo_cie10 VARCHAR(50),
    tratamiento TEXT,
    fecha_actualizacion DATETIME NOT NULL,
    INDEX idx_borrador_prof (profesional_id),
    INDEX idx_borrador_paciente (paciente_id),
    INDEX idx_borrador_turno (turno_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
