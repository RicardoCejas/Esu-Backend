-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V17: Origen de atenciones en portal del paciente y separación de centro emisor vs dispensación (Fase 3)
-- =============================================================================

-- 1. Ampliar receta_digital con centro emisor, farmacia dispensadora y lugar de retiro sugerido
ALTER TABLE receta_digital ADD COLUMN IF NOT EXISTS centro_salud_emisor_id BIGINT NULL;
ALTER TABLE receta_digital ADD COLUMN IF NOT EXISTS centro_salud_emisor_nombre VARCHAR(255) NULL;
ALTER TABLE receta_digital ADD COLUMN IF NOT EXISTS farmacia_dispensadora_nombre VARCHAR(255) NULL;
ALTER TABLE receta_digital ADD COLUMN IF NOT EXISTS lugar_retiro_sugerido VARCHAR(255) NULL;

CREATE INDEX idx_receta_centro_emisor ON receta_digital (centro_salud_emisor_id);

-- Poblar valores por defecto en recetas existentes
UPDATE receta_digital SET
    centro_salud_emisor_id = 1,
    centro_salud_emisor_nombre = 'Hospital Regional Aurelio Crespo',
    lugar_retiro_sugerido = 'Farmacia Central Hospital Regional Aurelio Crespo o Dispensario Barrial'
WHERE centro_salud_emisor_id IS NULL;

-- 2. Ampliar evolucion_medica con centro de salud de la atención
ALTER TABLE evolucion_medica ADD COLUMN IF NOT EXISTS centro_salud_id BIGINT NULL;
ALTER TABLE evolucion_medica ADD COLUMN IF NOT EXISTS centro_salud_nombre VARCHAR(255) NULL;

CREATE INDEX idx_evolucion_centro_salud ON evolucion_medica (centro_salud_id);

-- Actualizar evoluciones existentes desde turno o con centro central por defecto
UPDATE evolucion_medica em
LEFT JOIN turno t ON em.turno_id = t.id
SET 
    em.centro_salud_id = COALESCE(t.centro_salud_id, 1),
    em.centro_salud_nombre = COALESCE(t.centro_salud_nombre, 'Hospital Regional Aurelio Crespo')
WHERE em.centro_salud_id IS NULL;

-- 3. Ampliar historia_clinica con centro de salud de atención
ALTER TABLE historia_clinica ADD COLUMN IF NOT EXISTS centro_salud_id BIGINT NULL;
ALTER TABLE historia_clinica ADD COLUMN IF NOT EXISTS centro_salud_nombre VARCHAR(255) NULL;

UPDATE historia_clinica SET
    centro_salud_id = 1,
    centro_salud_nombre = 'Hospital Regional Aurelio Crespo'
WHERE centro_salud_id IS NULL;

-- 4. Ampliar estudios_adjuntos con centro de salud emisor y profesional
ALTER TABLE estudios_adjuntos ADD COLUMN IF NOT EXISTS centro_salud_id BIGINT NULL;
ALTER TABLE estudios_adjuntos ADD COLUMN IF NOT EXISTS centro_salud_nombre VARCHAR(255) NULL;
ALTER TABLE estudios_adjuntos ADD COLUMN IF NOT EXISTS profesional_nombre VARCHAR(255) NULL;

UPDATE estudios_adjuntos SET
    centro_salud_id = 1,
    centro_salud_nombre = 'Hospital Regional Aurelio Crespo'
WHERE centro_salud_id IS NULL;

-- 5. Ampliar paciente con centro de referencia habitual explícito
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS centro_salud_referencia_id BIGINT NULL;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS centro_salud_referencia_nombre VARCHAR(255) NULL;

UPDATE paciente SET
    centro_salud_referencia_id = 1,
    centro_salud_referencia_nombre = 'Hospital Regional Aurelio Crespo'
WHERE centro_salud_referencia_id IS NULL;
