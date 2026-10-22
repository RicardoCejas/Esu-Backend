-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V6: resumen clinico real, borradores persistidos y trazabilidad
-- Issue #120.
-- =============================================================================

-- Datos clinicos reales del paciente (los muestra el resumen clinico).
ALTER TABLE paciente ADD COLUMN alergias TEXT NULL AFTER grupo_sanguineo;
ALTER TABLE paciente ADD COLUMN antecedentes TEXT NULL AFTER alergias;

-- Datos de demostracion para el paciente semilla. No son valores fijos en el codigo.
UPDATE paciente
SET alergias = 'Penicilina y derivados beta-lactamicos (riesgo anafilactico); AINEs (ibuprofeno/aspirina) - broncoespasmo reactivo',
    antecedentes = 'Asma bronquial diagnosticada en 2018; Apendicectomia laparoscopica (2021); Vacunacion antigripal anual al dia'
WHERE id = 1;

UPDATE paciente
SET antecedentes = 'Hipertension arterial materna; Esquema de vacunacion completo para adultos'
WHERE id = 2;

-- Borrador de consulta autoguardado por paciente y profesional.
CREATE TABLE consulta_borrador (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    turno_id BIGINT NULL,
    contenido LONGTEXT NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME NOT NULL,
    INDEX idx_borrador_paciente_profesional (paciente_id, profesional_id),
    CONSTRAINT fk_borrador_paciente FOREIGN KEY (paciente_id) REFERENCES paciente (id) ON DELETE CASCADE,
    CONSTRAINT fk_borrador_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE CASCADE,
    CONSTRAINT fk_borrador_turno FOREIGN KEY (turno_id) REFERENCES turno (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Un turno no puede generar mas de una evolucion clinica.
-- Si una base existente tuviera duplicados, se desvinculan los duplicados antes
-- de crear el indice (la evolucion se conserva, solo se libera el turno).
UPDATE evolucion_medica e
JOIN (
    SELECT turno_id, MIN(id) AS conservar_id
    FROM evolucion_medica
    WHERE turno_id IS NOT NULL
    GROUP BY turno_id
    HAVING COUNT(*) > 1
) duplicados ON duplicados.turno_id = e.turno_id AND e.id <> duplicados.conservar_id
SET e.turno_id = NULL;

CREATE UNIQUE INDEX uk_evolucion_turno ON evolucion_medica (turno_id);
