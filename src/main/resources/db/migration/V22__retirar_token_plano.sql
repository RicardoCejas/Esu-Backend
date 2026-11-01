-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V22: Retirar almacenamiento de refresh tokens en texto plano
-- Cierre de transición: Los tokens solo se persisten y buscan como hash SHA-256
-- =============================================================================

DROP INDEX idx_refresh_token_token ON refresh_token;
ALTER TABLE refresh_token DROP COLUMN token;
