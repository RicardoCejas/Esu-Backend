-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V19: Tabla de Refresh Tokens para gestión de sesiones seguras
-- Con soporte de rotación, revocación y trazabilidad de tokens
-- =============================================================================

CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL UNIQUE,
    fecha_expiracion DATETIME NOT NULL,
    revocado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion DATETIME NOT NULL,
    reemplazado_por VARCHAR(128) NULL,
    INDEX idx_refresh_token_usuario (usuario_id),
    INDEX idx_refresh_token_token (token),
    CONSTRAINT fk_refresh_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
