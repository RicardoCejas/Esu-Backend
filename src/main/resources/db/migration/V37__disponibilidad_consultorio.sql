-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V37: Asociacion de consultorio habitual a disponibilidad profesional
-- =============================================================================

SET @col_disp_consultorio_id = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'disponibilidad_profesional'
      AND column_name = 'consultorio_id'
);
SET @add_disp_consultorio_id_stmt = IF(
    @col_disp_consultorio_id = 0,
    'ALTER TABLE disponibilidad_profesional ADD COLUMN consultorio_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt_add_disp_consultorio_id FROM @add_disp_consultorio_id_stmt;
EXECUTE stmt_add_disp_consultorio_id;
DEALLOCATE PREPARE stmt_add_disp_consultorio_id;
