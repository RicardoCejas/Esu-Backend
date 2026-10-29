-- =============================================================================
-- Migración V15: Columnas de lease y backoff para procesamiento outbox concurrente
-- =============================================================================

ALTER TABLE outbox_evento ADD COLUMN IF NOT EXISTS lease_hasta DATETIME NULL;
ALTER TABLE outbox_evento ADD COLUMN IF NOT EXISTS proximo_intento DATETIME NULL;

CREATE INDEX IF NOT EXISTS idx_outbox_reclamo ON outbox_evento (estado, proximo_intento, lease_hasta);
