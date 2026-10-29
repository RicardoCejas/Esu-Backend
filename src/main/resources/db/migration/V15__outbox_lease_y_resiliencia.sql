-- =============================================================================
-- Migración V15: Columnas de lease, idempotencia y resiliencia para Outbox y Turnos
-- =============================================================================

ALTER TABLE outbox_evento ADD COLUMN IF NOT EXISTS lease_hasta DATETIME NULL;
ALTER TABLE outbox_evento ADD COLUMN IF NOT EXISTS proximo_intento DATETIME NULL;
ALTER TABLE outbox_evento ADD COLUMN IF NOT EXISTS despachado_externo BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_outbox_reclamo ON outbox_evento (estado, proximo_intento, lease_hasta);

-- Clave de idempotencia para prevenir turnos duplicados por reintentos de clientes
ALTER TABLE turno ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64) NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_turno_idempotency ON turno (idempotency_key);
