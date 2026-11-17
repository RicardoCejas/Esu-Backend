-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V28: Consolidacion de historia clinica unica, auditoria de descartes
-- y retencion legal sin borrado en cascada (Ley 26.529).
-- Issue #217.
-- =============================================================================

-- 1. Crear tabla de auditoria para registrar cabeceras de HC descartadas o consolidadas
CREATE TABLE IF NOT EXISTS historia_clinica_auditoria_descartes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    historia_clinica_id BIGINT NOT NULL,
    paciente_id BIGINT NOT NULL,
    motivo_consulta TEXT,
    diagnostico TEXT,
    tratamiento TEXT,
    fecha_atencion DATETIME,
    fecha_descarte DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo_descarte VARCHAR(255) DEFAULT 'Consolidacion de historia clinica unica (Issue #217)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Respaldar en auditoria cualquier historia clinica duplicada preexistente
INSERT INTO historia_clinica_auditoria_descartes (historia_clinica_id, paciente_id, motivo_consulta, diagnostico, tratamiento, fecha_atencion)
SELECT h.id, h.paciente_id, h.motivo_consulta, h.diagnostico, h.tratamiento, h.fecha_atencion
FROM historia_clinica h
WHERE h.paciente_id IS NOT NULL
  AND h.id NOT IN (
      SELECT keep_id FROM (
          SELECT MAX(id) AS keep_id FROM historia_clinica WHERE paciente_id IS NOT NULL GROUP BY paciente_id
      ) AS hc_keep
  );

-- 3. Reasignar cualquier evolucion que apunte a una HC duplicada hacia la HC definitiva
UPDATE evolucion_medica e
JOIN historia_clinica h ON e.historia_clinica_id = h.id
JOIN (
    SELECT paciente_id, MAX(id) AS keep_id FROM historia_clinica WHERE paciente_id IS NOT NULL GROUP BY paciente_id
) k ON h.paciente_id = k.paciente_id
SET e.historia_clinica_id = k.keep_id
WHERE e.id > 0
  AND e.historia_clinica_id <> k.keep_id;

-- 4. Eliminar las cabeceras duplicadas (su informacion clinica ya esta preservada en auditoria)
DELETE FROM historia_clinica
WHERE id > 0
  AND paciente_id IS NOT NULL
  AND id NOT IN (
      SELECT keep_id FROM (
          SELECT MAX(id) AS keep_id FROM historia_clinica WHERE paciente_id IS NOT NULL GROUP BY paciente_id
      ) AS hc_keep
  );

-- 5. Asegurar que todo paciente con evoluciones cuente con su cabecera unica de historia clinica
INSERT INTO historia_clinica (paciente_id, fecha_atencion, motivo_consulta, diagnostico, tratamiento)
SELECT DISTINCT e.paciente_id, NOW(), 'Apertura de historia clinica consolidada', 'Consolidacion historica', 'Seguimiento general'
FROM evolucion_medica e
WHERE e.paciente_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM historia_clinica hc WHERE hc.paciente_id = e.paciente_id);

-- 6. Vincular evoluciones huerfanas (historia_clinica_id IS NULL) a la historia clinica unica de su paciente
UPDATE evolucion_medica e
JOIN historia_clinica hc ON e.paciente_id = hc.paciente_id
SET e.historia_clinica_id = hc.id
WHERE e.id > 0
  AND e.historia_clinica_id IS NULL;

-- 7. Eliminar cascadas destructivas y aplicar ON DELETE RESTRICT para cumplimiento Ley 26.529

-- 7.1 paciente -> usuario (evitar que borrar usuario elimine paciente)
SET @fk_pac_usr = (
    SELECT constraint_name
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'paciente'
      AND column_name = 'fk_usuario'
      AND referenced_table_name = 'usuario'
    ORDER BY (constraint_name = 'fk_paciente_usuario') DESC
    LIMIT 1
);
SET @drop_pac_usr_stmt = IF(@fk_pac_usr IS NOT NULL, CONCAT('ALTER TABLE paciente DROP FOREIGN KEY `', @fk_pac_usr, '`'), 'SELECT 1');
PREPARE stmt_drop_pac_usr FROM @drop_pac_usr_stmt;
EXECUTE stmt_drop_pac_usr;
DEALLOCATE PREPARE stmt_drop_pac_usr;

ALTER TABLE paciente
    ADD CONSTRAINT fk_paciente_usuario FOREIGN KEY (fk_usuario) REFERENCES usuario (id) ON DELETE RESTRICT;

-- 7.2 historia_clinica -> paciente (evitar que borrar paciente elimine historia clinica)
SET @fk_hc_pac = (
    SELECT constraint_name
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'historia_clinica'
      AND column_name = 'paciente_id'
      AND referenced_table_name = 'paciente'
    ORDER BY (constraint_name = 'fk_hc_paciente') DESC
    LIMIT 1
);
SET @drop_hc_pac_stmt = IF(@fk_hc_pac IS NOT NULL, CONCAT('ALTER TABLE historia_clinica DROP FOREIGN KEY `', @fk_hc_pac, '`'), 'SELECT 1');
PREPARE stmt_drop_hc_pac FROM @drop_hc_pac_stmt;
EXECUTE stmt_drop_hc_pac;
DEALLOCATE PREPARE stmt_drop_hc_pac;

ALTER TABLE historia_clinica
    ADD CONSTRAINT fk_hc_paciente FOREIGN KEY (paciente_id) REFERENCES paciente (id) ON DELETE RESTRICT;

-- 7.3 evolucion_medica -> paciente (evitar borrado de evoluciones)
SET @fk_ev_pac = (
    SELECT constraint_name
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'evolucion_medica'
      AND column_name = 'paciente_id'
      AND referenced_table_name = 'paciente'
    ORDER BY (constraint_name = 'fk_evolucion_paciente') DESC
    LIMIT 1
);
SET @drop_ev_pac_stmt = IF(@fk_ev_pac IS NOT NULL, CONCAT('ALTER TABLE evolucion_medica DROP FOREIGN KEY `', @fk_ev_pac, '`'), 'SELECT 1');
PREPARE stmt_drop_ev_pac FROM @drop_ev_pac_stmt;
EXECUTE stmt_drop_ev_pac;
DEALLOCATE PREPARE stmt_drop_ev_pac;

ALTER TABLE evolucion_medica
    ADD CONSTRAINT fk_evolucion_paciente FOREIGN KEY (paciente_id) REFERENCES paciente (id) ON DELETE RESTRICT;

-- 7.4 evolucion_medica -> historia_clinica (evitar borrado de evoluciones al tocar HC)
SET @fk_ev_hc = (
    SELECT constraint_name
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'evolucion_medica'
      AND column_name = 'historia_clinica_id'
      AND referenced_table_name = 'historia_clinica'
    ORDER BY (constraint_name = 'fk_evolucion_hc') DESC
    LIMIT 1
);
SET @drop_ev_hc_stmt = IF(@fk_ev_hc IS NOT NULL, CONCAT('ALTER TABLE evolucion_medica DROP FOREIGN KEY `', @fk_ev_hc, '`'), 'SELECT 1');
PREPARE stmt_drop_ev_hc FROM @drop_ev_hc_stmt;
EXECUTE stmt_drop_ev_hc;
DEALLOCATE PREPARE stmt_drop_ev_hc;

ALTER TABLE evolucion_medica
    ADD CONSTRAINT fk_evolucion_hc FOREIGN KEY (historia_clinica_id) REFERENCES historia_clinica (id) ON DELETE RESTRICT;

-- 7.5 receta_digital -> paciente
SET @fk_rec_pac = (
    SELECT constraint_name
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'receta_digital'
      AND column_name = 'paciente_id'
      AND referenced_table_name = 'paciente'
    ORDER BY (constraint_name = 'fk_receta_paciente') DESC
    LIMIT 1
);
SET @drop_rec_pac_stmt = IF(@fk_rec_pac IS NOT NULL, CONCAT('ALTER TABLE receta_digital DROP FOREIGN KEY `', @fk_rec_pac, '`'), 'SELECT 1');
PREPARE stmt_drop_rec_pac FROM @drop_rec_pac_stmt;
EXECUTE stmt_drop_rec_pac;
DEALLOCATE PREPARE stmt_drop_rec_pac;

ALTER TABLE receta_digital
    ADD CONSTRAINT fk_receta_paciente FOREIGN KEY (paciente_id) REFERENCES paciente (id) ON DELETE RESTRICT;
