-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V37: Asociacion de consultorio habitual a disponibilidad profesional
-- =============================================================================

ALTER TABLE disponibilidad_profesional ADD COLUMN consultorio_id BIGINT NULL;
