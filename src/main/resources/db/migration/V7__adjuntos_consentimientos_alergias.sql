-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V7: Adjuntos Médicos, Consentimientos Informados y Alergias (Sprint 6)
-- HU-101 (Adjuntos), HU-102 (Timeline Optimizado), HU-103 (Consentimientos), HU-104 (Alergias)
-- =============================================================================

CREATE TABLE IF NOT EXISTS alergias_pacientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    sustancia_alergeno VARCHAR(150) NOT NULL,
    severidad VARCHAR(20) NOT NULL,
    fecha_diagnostico DATE NULL,
    observaciones VARCHAR(255) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_alergia_paciente (paciente_id),
    INDEX idx_alergia_sustancia (sustancia_alergeno)
);

CREATE TABLE IF NOT EXISTS consentimientos_informados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    procedimiento VARCHAR(200) NOT NULL,
    texto_legal LONGTEXT NOT NULL,
    firma_digital_base64 LONGTEXT NOT NULL,
    ip_firma VARCHAR(45) NOT NULL,
    hash_integridad_sha256 VARCHAR(64) NOT NULL,
    fecha_hora_firma DATETIME NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'FIRMADO',
    INDEX idx_consentimiento_paciente (paciente_id),
    INDEX idx_consentimiento_profesional (profesional_id),
    INDEX idx_consentimiento_hash (hash_integridad_sha256)
);

CREATE TABLE IF NOT EXISTS estudios_adjuntos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    evolucion_id BIGINT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL,
    ruta_almacenamiento VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    tamanio_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    tipo_estudio VARCHAR(50) NOT NULL DEFAULT 'INFORME',
    fecha_estudio DATE NULL,
    fecha_carga DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_estudio_paciente (paciente_id),
    INDEX idx_estudio_evolucion (evolucion_id),
    INDEX idx_estudio_checksum (checksum_sha256)
);

-- Semillas iniciales de alergias críticas para pacientes de prueba
INSERT INTO alergias_pacientes (paciente_id, sustancia_alergeno, severidad, fecha_diagnostico, observaciones, activo, fecha_registro)
VALUES
(1, 'Penicilina', 'GRAVE', '2023-04-12', 'Riesgo de shock anafiláctico con antibióticos beta-lactámicos', TRUE, NOW()),
(1, 'Ibuprofeno', 'MODERADA', '2022-09-05', 'Broncoespasmo reactivo y erupción cutánea', TRUE, NOW()),
(2, 'Dipirona', 'GRAVE', '2021-11-20', 'Hipotensión severa y edema angioneurótico', TRUE, NOW())
ON DUPLICATE KEY UPDATE sustancia_alergeno = VALUES(sustancia_alergeno);
