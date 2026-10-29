-- =============================================================================
-- Migración V10: Revocación de sesiones (token_version), Outbox pattern y
-- auditoría de accesos de emergencia a Historia Clínica.
-- =============================================================================

-- 1. Agregar versión de credenciales para revocación de JWT tras cambio de clave/roles
ALTER TABLE usuario ADD COLUMN token_version INT NOT NULL DEFAULT 1;

-- 2. Agregar motivo de acceso a log_acceso_hc para break-glass / emergencias
ALTER TABLE log_acceso_hc ADD COLUMN motivo VARCHAR(500) NULL;

-- 3. Tabla para tokens aleatorios y seguros de verificación de documentos clínicos
CREATE TABLE IF NOT EXISTS token_verificacion_documento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    paciente_id BIGINT NOT NULL,
    tipo_documento VARCHAR(50) NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    revocado BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_token_verif_doc (token),
    INDEX idx_token_paciente (paciente_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Tabla para patrón Outbox (eventos de dominio post-commit con reintentos)
CREATE TABLE IF NOT EXISTS outbox_evento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_evento VARCHAR(100) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos INT NOT NULL DEFAULT 0,
    ultimo_error TEXT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME NULL,
    INDEX idx_outbox_estado (estado, fecha_creacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
