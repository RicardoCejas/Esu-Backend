-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V34: Cantidad de plantas fisicas y estudios diagnosticos por sede
-- =============================================================================

ALTER TABLE centro_salud ADD COLUMN cantidad_plantas INT NOT NULL DEFAULT 1;
ALTER TABLE centro_salud ADD COLUMN estudios_habilitados VARCHAR(1000) NULL;

-- Configuracion inicial consistente con las sedes activas y consultorios semilla (V18 / V24)
UPDATE centro_salud
SET cantidad_plantas = 4,
    estudios_habilitados = 'Laboratorio de Análisis Clínicos|Radiografía Digital|Ecografía General|Ecografía Mamaria'
WHERE id = 1;

UPDATE centro_salud
SET cantidad_plantas = 2,
    estudios_habilitados = 'Laboratorio de Análisis Clínicos|Radiografía Digital|Ecografía General'
WHERE id = 2;

UPDATE centro_salud
SET cantidad_plantas = 3,
    estudios_habilitados = 'Laboratorio de Análisis Clínicos|Radiografía Digital|Ecografía General|Ecografía Mamaria'
WHERE id = 3;

UPDATE centro_salud
SET cantidad_plantas = 2,
    estudios_habilitados = ''
WHERE id = 5;
