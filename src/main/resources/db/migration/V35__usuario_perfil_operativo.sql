-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V35: Datos operativos de identidad (nombre, apellido, dni) en usuario
-- =============================================================================

SET @dbname = DATABASE();
SET @tablename = 'usuario';

-- Columna nombre (idempotente)
SET @col = 'nombre';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @col
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @col, ' VARCHAR(100) NULL')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Columna apellido (idempotente)
SET @col = 'apellido';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @col
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @col, ' VARCHAR(100) NULL')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Columna dni (idempotente)
SET @col = 'dni';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @col
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @col, ' VARCHAR(20) NULL')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Backfill inicial para cuentas profesionales vinculadas sin datos en tabla usuario
UPDATE usuario u
SET nombre = (SELECT p.nombre FROM profesional p WHERE p.usuario_id = u.id LIMIT 1),
    apellido = (SELECT p.apellido FROM profesional p WHERE p.usuario_id = u.id LIMIT 1)
WHERE u.nombre IS NULL
  AND EXISTS (SELECT 1 FROM profesional p WHERE p.usuario_id = u.id);

-- Backfill inicial para cuentas de pacientes vinculadas sin datos en tabla usuario
UPDATE usuario u
SET nombre = (SELECT pac.nombre FROM paciente pac WHERE pac.fk_usuario = u.id LIMIT 1),
    apellido = (SELECT pac.apellido FROM paciente pac WHERE pac.fk_usuario = u.id LIMIT 1),
    dni = (SELECT pac.dni FROM paciente pac WHERE pac.fk_usuario = u.id LIMIT 1)
WHERE u.nombre IS NULL
  AND EXISTS (SELECT 1 FROM paciente pac WHERE pac.fk_usuario = u.id);
