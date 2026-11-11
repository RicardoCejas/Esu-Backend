-- V36: Agregar columna planta en tabla turno para trazabilidad de infraestructura y turnos de mostrador
SET @col_turno_planta = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'turno'
      AND column_name = 'planta'
);
SET @add_turno_planta_stmt = IF(
    @col_turno_planta = 0,
    'ALTER TABLE turno ADD COLUMN planta VARCHAR(50) NULL',
    'SELECT 1'
);
PREPARE stmt_add_turno_planta FROM @add_turno_planta_stmt;
EXECUTE stmt_add_turno_planta;
DEALLOCATE PREPARE stmt_add_turno_planta;
