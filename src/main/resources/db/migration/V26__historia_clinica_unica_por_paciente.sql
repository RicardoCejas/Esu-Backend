-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V26: historia clinica unica por paciente
-- Issue #217.
-- =============================================================================

CREATE TEMPORARY TABLE hc_keep AS
SELECT paciente_id, MAX(id) AS keep_id
FROM historia_clinica
WHERE paciente_id IS NOT NULL
GROUP BY paciente_id;

UPDATE evolucion_medica e
JOIN historia_clinica h ON e.historia_clinica_id = h.id
JOIN hc_keep k ON h.paciente_id = k.paciente_id
SET e.historia_clinica_id = k.keep_id;

DELETE h
FROM historia_clinica h
JOIN hc_keep k ON h.paciente_id = k.paciente_id
WHERE h.id <> k.keep_id;

DROP TEMPORARY TABLE hc_keep;

ALTER TABLE historia_clinica
    ADD CONSTRAINT uk_historia_clinica_paciente UNIQUE (paciente_id);
