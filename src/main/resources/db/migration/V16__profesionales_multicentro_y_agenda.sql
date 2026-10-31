-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V16: Soporte de múltiples centros por profesional y trazabilidad de sedes en turnos (Fase 2)
-- =============================================================================

-- 1. Tabla de vinculación muchos a muchos entre profesional y centros de salud
CREATE TABLE IF NOT EXISTS profesional_centro_salud (
    profesional_id BIGINT NOT NULL,
    centro_salud_id BIGINT NOT NULL,
    PRIMARY KEY (profesional_id, centro_salud_id),
    INDEX idx_pcs_centro (centro_salud_id),
    CONSTRAINT fk_pcs_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Poblar vinculaciones iniciales a partir de disponibilidades existentes
INSERT IGNORE INTO profesional_centro_salud (profesional_id, centro_salud_id)
SELECT DISTINCT profesional_id, centro_salud_id 
FROM disponibilidad_profesional 
WHERE centro_salud_id IS NOT NULL;

-- 3. Asignar sede central (Hospital Regional Aurelio Crespo - ID: 1) por defecto a profesionales existentes
INSERT IGNORE INTO profesional_centro_salud (profesional_id, centro_salud_id)
SELECT id, 1 FROM profesional;

-- 4. Ampliar la tabla de turnos con identificación explícita de sede y consultorio
ALTER TABLE turno ADD COLUMN IF NOT EXISTS centro_salud_id BIGINT NULL;
ALTER TABLE turno ADD COLUMN IF NOT EXISTS centro_salud_nombre VARCHAR(255) NULL;
ALTER TABLE turno ADD COLUMN IF NOT EXISTS consultorio_numero VARCHAR(50) NULL;

-- 5. Actualizar turnos existentes con sede por defecto para garantizar retrocompatibilidad
UPDATE turno SET 
    centro_salud_id = 1, 
    centro_salud_nombre = 'Hospital Regional Aurelio Crespo' 
WHERE centro_salud_id IS NULL;

-- 6. Índice para optimizar consultas de agenda por profesional, centro y fecha
CREATE INDEX idx_turno_prof_centro_fecha ON turno (profesional_id, centro_salud_id, fecha_hora);
