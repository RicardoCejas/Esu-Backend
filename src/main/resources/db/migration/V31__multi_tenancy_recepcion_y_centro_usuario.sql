-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V31: Aislamiento institucional y multi-tenancy estricto en usuarios Recepcionista (HU-Backend-13)
-- =============================================================================

-- 1. Agregar columnas centro_salud_id y centro_salud_nombre a la tabla usuario
ALTER TABLE usuario ADD COLUMN centro_salud_id BIGINT NULL;
ALTER TABLE usuario ADD COLUMN centro_salud_nombre VARCHAR(180) NULL;

-- 2. Indice para consultas y filtros por centro de salud
CREATE INDEX idx_usuario_centro ON usuario(centro_salud_id);

-- 3. Clave foranea referencial hacia centro_salud (SET NULL si se diera de baja la sede)
ALTER TABLE usuario ADD CONSTRAINT fk_usuario_centro_salud 
    FOREIGN KEY (centro_salud_id) REFERENCES centro_salud(id) ON DELETE SET NULL;

-- 4. Asignacion institucional canonica de las cuentas de recepcion existentes
UPDATE usuario 
SET centro_salud_id = 1, centro_salud_nombre = 'Hospital Provincial Dr. Aurelio Crespo' 
WHERE email = 'crespo.institucional@esu.salud.gob.ar';

UPDATE usuario 
SET centro_salud_id = 2, centro_salud_nombre = 'Clínica Cabrera' 
WHERE email = 'cabrera.institucional@esu.salud.gob.ar';

UPDATE usuario 
SET centro_salud_id = 3, centro_salud_nombre = 'Centro de Diagnóstico por Imágenes Cruz del Eje' 
WHERE email = 'diagnostico.institucional@esu.salud.gob.ar';

UPDATE usuario 
SET centro_salud_id = 5, centro_salud_nombre = 'Sanatorio María Isabel' 
WHERE email = 'mariaisabel.institucional@esu.salud.gob.ar';

UPDATE usuario 
SET centro_salud_id = 1, centro_salud_nombre = 'Hospital Provincial Dr. Aurelio Crespo' 
WHERE email = 'recepcion@esu.gob.ar';
