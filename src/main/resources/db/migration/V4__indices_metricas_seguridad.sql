-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V4: Índices de Métricas Sanitarias y Seguridad (HU-98, HU-99, HU-100)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Tabla de Tokens Criptográficos de Recuperación de Contraseña (HU-99)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS token_recuperacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_token_recuperacion_token ON token_recuperacion (token);
CREATE INDEX idx_token_recuperacion_usuario ON token_recuperacion (usuario_id);

-- -----------------------------------------------------------------------------
-- 2. Tabla de Logs de Auditoría de Seguridad de Sesiones e IPs (HU-100)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS log_auditoria_seguridad (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(100),
    user_agent VARCHAR(500),
    evento VARCHAR(50) NOT NULL,
    detalle VARCHAR(500),
    fecha DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_auditoria_email_fecha ON log_auditoria_seguridad (email, fecha);
CREATE INDEX idx_auditoria_ip_fecha ON log_auditoria_seguridad (ip_address, fecha);
CREATE INDEX idx_auditoria_fecha ON log_auditoria_seguridad (fecha);

-- -----------------------------------------------------------------------------
-- 3. Índices Optimizados para Métricas Sanitarias y Ausentismo (HU-98)
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_turno_fecha_estado ON turno (fecha_hora, estado);
