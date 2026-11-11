-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V35: Datos operativos de identidad (nombre, apellido, dni) en usuario
-- =============================================================================

-- 1. Agregar columnas de forma idempotente (resiliente ante DDL auto-committed en MySQL + flyway.repair)
SET @col_usuario_nombre = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'usuario'
      AND column_name = 'nombre'
);
SET @add_usuario_nombre_stmt = IF(
    @col_usuario_nombre = 0,
    'ALTER TABLE usuario ADD COLUMN nombre VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt_add_usuario_nombre FROM @add_usuario_nombre_stmt;
EXECUTE stmt_add_usuario_nombre;
DEALLOCATE PREPARE stmt_add_usuario_nombre;

SET @col_usuario_apellido = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'usuario'
      AND column_name = 'apellido'
);
SET @add_usuario_apellido_stmt = IF(
    @col_usuario_apellido = 0,
    'ALTER TABLE usuario ADD COLUMN apellido VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt_add_usuario_apellido FROM @add_usuario_apellido_stmt;
EXECUTE stmt_add_usuario_apellido;
DEALLOCATE PREPARE stmt_add_usuario_apellido;

SET @col_usuario_dni = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'usuario'
      AND column_name = 'dni'
);
SET @add_usuario_dni_stmt = IF(
    @col_usuario_dni = 0,
    'ALTER TABLE usuario ADD COLUMN dni VARCHAR(20) NULL',
    'SELECT 1'
);
PREPARE stmt_add_usuario_dni FROM @add_usuario_dni_stmt;
EXECUTE stmt_add_usuario_dni;
DEALLOCATE PREPARE stmt_add_usuario_dni;

-- 2. Backfill inicial para cuentas profesionales vinculadas sin datos en tabla usuario
UPDATE usuario u
JOIN profesional p ON p.usuario_id = u.id
SET u.nombre = COALESCE(u.nombre, p.nombre),
    u.apellido = COALESCE(u.apellido, p.apellido)
WHERE u.id > 0
  AND (u.nombre IS NULL OR u.apellido IS NULL);

-- 3. Backfill inicial para cuentas de pacientes vinculadas sin datos en tabla usuario
UPDATE usuario u
JOIN paciente pac ON pac.fk_usuario = u.id
SET u.nombre = COALESCE(u.nombre, pac.nombre),
    u.apellido = COALESCE(u.apellido, pac.apellido),
    u.dni = COALESCE(u.dni, pac.dni)
WHERE u.id > 0
  AND (u.nombre IS NULL OR u.apellido IS NULL OR u.dni IS NULL);
