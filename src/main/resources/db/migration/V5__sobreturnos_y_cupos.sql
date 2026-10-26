-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V5: Soporte para Sobreturnos y Demanda Espontánea (HU-BE-91)
-- =============================================================================

ALTER TABLE turno ADD COLUMN IF NOT EXISTS es_sobreturno BOOLEAN DEFAULT FALSE;
ALTER TABLE turno ADD COLUMN IF NOT EXISTS motivo_sobreturno VARCHAR(255);

CREATE INDEX idx_turno_sobreturno ON turno (profesional_id, fecha_hora, es_sobreturno);
