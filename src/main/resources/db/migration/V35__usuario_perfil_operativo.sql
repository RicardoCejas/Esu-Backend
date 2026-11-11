-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V35: Datos operativos de identidad (nombre, apellido, dni) en usuario
-- =============================================================================

ALTER TABLE usuario ADD COLUMN nombre VARCHAR(100) NULL;
ALTER TABLE usuario ADD COLUMN apellido VARCHAR(100) NULL;
ALTER TABLE usuario ADD COLUMN dni VARCHAR(20) NULL;

-- Backfill inicial para cuentas profesionales vinculadas sin datos en tabla usuario
UPDATE usuario u
SET nombre = (SELECT p.nombre FROM profesional p WHERE p.usuario_id = u.id LIMIT 1),
    apellido = (SELECT p.apellido FROM profesional p WHERE p.usuario_id = u.id LIMIT 1)
WHERE u.nombre IS NULL
  AND EXISTS (SELECT 1 FROM profesional p WHERE p.usuario_id = u.id);

-- Backfill inicial para cuentas de pacientes vinculadas sin datos en tabla usuario
UPDATE usuario u
SET nombre = (SELECT pac.nombre FROM paciente pac WHERE pac.usuario_id = u.id LIMIT 1),
    apellido = (SELECT pac.apellido FROM paciente pac WHERE pac.usuario_id = u.id LIMIT 1),
    dni = (SELECT pac.dni FROM paciente pac WHERE pac.usuario_id = u.id LIMIT 1)
WHERE u.nombre IS NULL
  AND EXISTS (SELECT 1 FROM paciente pac WHERE pac.usuario_id = u.id);
