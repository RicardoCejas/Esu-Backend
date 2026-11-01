-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V21: Almacenamiento seguro de Refresh Tokens mediante hash criptográfico SHA-256
-- Compatible con MySQL 8.0 y motores relacionales estándar
-- =============================================================================

ALTER TABLE refresh_token ADD COLUMN token_hash VARCHAR(64) NULL AFTER token;

-- Calcular hash SHA-256 para tokens existentes si los hubiera
UPDATE refresh_token SET token_hash = SHA2(token, 256) WHERE token_hash IS NULL AND token IS NOT NULL;

-- Forzar obligatoriedad e índice único sobre el hash del token
ALTER TABLE refresh_token MODIFY COLUMN token_hash VARCHAR(64) NOT NULL;
CREATE UNIQUE INDEX idx_refresh_token_hash ON refresh_token (token_hash);
