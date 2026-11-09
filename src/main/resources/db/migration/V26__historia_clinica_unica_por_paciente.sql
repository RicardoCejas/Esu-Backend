-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V26: historia clinica unica por paciente
-- Issue #217.
-- =============================================================================

-- Consolidar evoluciones y depurar cabeceras duplicadas sin crear tablas
-- temporales sin clave primaria (compatible con sql_require_primary_key = ON
-- y enforce_gtid_consistency = ON en MySQL gestionado).
UPDATE evolucion_medica e
JOIN historia_clinica h ON e.historia_clinica_id = h.id
JOIN (
    SELECT paciente_id, MAX(id) AS keep_id
    FROM historia_clinica
    WHERE paciente_id IS NOT NULL
    GROUP BY paciente_id
) k ON h.paciente_id = k.paciente_id
SET e.historia_clinica_id = k.keep_id
WHERE e.id > 0
  AND e.historia_clinica_id <> k.keep_id;

DELETE FROM historia_clinica
WHERE id > 0
  AND paciente_id IS NOT NULL
  AND id NOT IN (
      SELECT keep_id FROM (
          SELECT MAX(id) AS keep_id
          FROM historia_clinica
          WHERE paciente_id IS NOT NULL
          GROUP BY paciente_id
      ) AS hc_keep
  );

SET @uk_hc_pac = (
    SELECT constraint_name
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'historia_clinica'
      AND constraint_name = 'uk_historia_clinica_paciente'
    LIMIT 1
);
SET @add_uk_hc_stmt = IF(
    @uk_hc_pac IS NULL,
    'ALTER TABLE historia_clinica ADD CONSTRAINT uk_historia_clinica_paciente UNIQUE (paciente_id)',
    'SELECT 1'
);
PREPARE stmt_add_uk_hc FROM @add_uk_hc_stmt;
EXECUTE stmt_add_uk_hc;
DEALLOCATE PREPARE stmt_add_uk_hc;
