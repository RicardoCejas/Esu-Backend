-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V29: Endurecimiento de Refresh Tokens (familias), Almacén de Revocación de Access Tokens y Tokens Temporales 2FA
-- Issue #219 (HU-Backend-11)
-- =============================================================================

-- 1. Agregar identificación de familia (token_family_id) a refresh_token para rotación segura
ALTER TABLE refresh_token ADD COLUMN token_family_id VARCHAR(64) NULL AFTER usuario_id;

-- Inicializar familia con token_hash para tokens preexistentes
UPDATE refresh_token SET token_family_id = token_hash WHERE token_family_id IS NULL AND id > 0;

-- Exigir no nulidad e índice sobre la familia de refresh tokens
ALTER TABLE refresh_token MODIFY COLUMN token_family_id VARCHAR(64) NOT NULL;
CREATE INDEX idx_refresh_token_family ON refresh_token (token_family_id);

-- 2. Almacén persistente de tokens de acceso revocados (Access Token Blocklist)
CREATE TABLE IF NOT EXISTS token_revocado (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    jti VARCHAR(64) NOT NULL UNIQUE,
    fecha_expiracion DATETIME NOT NULL,
    fecha_revocacion DATETIME NOT NULL,
    razon VARCHAR(64) NOT NULL,
    INDEX idx_token_revocado_jti (jti),
    INDEX idx_token_revocado_expiracion (fecha_expiracion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Tokens temporales de 2FA de un solo uso (Single-use atomic consumption)
CREATE TABLE IF NOT EXISTS two_factor_temp_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    jti VARCHAR(64) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL,
    consumido BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion DATETIME NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    fecha_consumo DATETIME NULL,
    INDEX idx_2fa_temp_jti (jti),
    INDEX idx_2fa_temp_usuario (usuario_id),
    CONSTRAINT fk_2fa_temp_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
