-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V27: autenticacion de dos factores
-- Issue #219.
-- =============================================================================

ALTER TABLE usuario ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN two_factor_secret VARCHAR(255) NULL;
ALTER TABLE usuario ADD COLUMN two_factor_backup_codes TEXT NULL;
ALTER TABLE usuario ADD COLUMN two_factor_last_counter BIGINT NULL;
