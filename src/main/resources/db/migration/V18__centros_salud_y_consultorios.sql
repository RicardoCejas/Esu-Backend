-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V18: Persistencia real de Centros de Salud, Consultorios Físicos
-- y descripción de Especialidades (Fase 2 - Contratos e Integración)
-- =============================================================================

-- 1. Tabla de Centros de Salud / Sedes Asistenciales
CREATE TABLE IF NOT EXISTS centro_salud (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(180) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    telefono VARCHAR(80) NULL,
    ciudad VARCHAR(100) NOT NULL DEFAULT 'Cruz del Eje',
    horario_atencion VARCHAR(180) NULL,
    disponible_guardia BOOLEAN NOT NULL DEFAULT FALSE,
    latitud DOUBLE NULL,
    longitud DOUBLE NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_centro_salud_tipo (tipo),
    INDEX idx_centro_salud_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tabla de Consultorios Físicos por Centro de Salud
CREATE TABLE IF NOT EXISTS consultorio_fisico (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    centro_salud_id BIGINT NOT NULL,
    numero VARCHAR(50) NOT NULL,
    planta VARCHAR(80) NOT NULL DEFAULT 'Planta Baja',
    especialidad_asignada VARCHAR(150) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_consultorio_centro (centro_salud_id),
    CONSTRAINT fk_consultorio_centro FOREIGN KEY (centro_salud_id) REFERENCES centro_salud (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Semillas oficiales de Centros de Salud de Cruz del Eje (IDs 1..4 coherentes con V16/V17)
INSERT INTO centro_salud (id, nombre, tipo, direccion, telefono, ciudad, horario_atencion, disponible_guardia, latitud, longitud, activo) VALUES
(1, 'Hospital Regional Aurelio Crespo', 'HOSPITAL', 'Av. Juan B. Justo 1200, Cruz del Eje', '03549-422001', 'Cruz del Eje', '24 Horas (Guardia permanente)', TRUE, -30.7312, -64.7985, TRUE),
(2, 'Clínica Privada Cabrera', 'CLINICA', 'Sarmiento 450, Cruz del Eje', '03549-423110', 'Cruz del Eje', 'Lunes a Sábados 08:00 a 20:00', TRUE, -30.7268, -64.8031, TRUE),
(3, 'Centro de Diagnóstico Cruz del Eje', 'CONSULTORIO_PRIVADO', 'Rivadavia 210, Cruz del Eje', '03549-421550', 'Cruz del Eje', 'Lunes a Viernes 07:30 a 20:00', FALSE, -30.7254, -64.8058, TRUE),
(4, 'Dispensario Municipal Santa Rosa', 'DISPENSARIO', 'Calle Pública s/n, B° Santa Rosa, Cruz del Eje', '03549-425100', 'Cruz del Eje', 'Lunes a Viernes 07:00 a 14:00', FALSE, -30.7385, -64.7910, TRUE)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    tipo = VALUES(tipo),
    direccion = VALUES(direccion),
    telefono = VALUES(telefono),
    ciudad = VALUES(ciudad),
    horario_atencion = VALUES(horario_atencion),
    disponible_guardia = VALUES(disponible_guardia);

-- 4. Semillas iniciales de Consultorios Físicos
INSERT INTO consultorio_fisico (id, centro_salud_id, numero, planta, especialidad_asignada, activo) VALUES
(1, 1, '101', 'Planta Baja', 'Clínica Médica / Medicina General', TRUE),
(2, 1, '102', 'Planta Baja', 'Clínica Médica / Medicina General', TRUE),
(3, 1, '105', 'Planta Baja', 'Pediatría y Neonatología', TRUE),
(4, 1, '204', '1° Piso', 'Cardiología Clínica', TRUE),
(5, 2, '1', 'Planta Única', 'Clínica Médica / Medicina General', TRUE),
(6, 3, '301', '2° Piso', 'Ginecología y Obstetricia', TRUE)
ON DUPLICATE KEY UPDATE
    numero = VALUES(numero),
    planta = VALUES(planta),
    especialidad_asignada = VALUES(especialidad_asignada);

-- 5. Ampliar Especialidad con descripción opcional
ALTER TABLE especialidad ADD COLUMN descripcion VARCHAR(500) NULL;

UPDATE especialidad SET descripcion = 'Atención primaria integral para adultos, chequeos clínicos y seguimiento de patologías crónicas.' WHERE id = 1 AND descripcion IS NULL;
UPDATE especialidad SET descripcion = 'Atención integral del recién nacido, lactante, niños y adolescentes.' WHERE id = 2 AND descripcion IS NULL;
UPDATE especialidad SET descripcion = 'Prevención cardiovascular, electrocardiogramas, Holter y control de hipertensión.' WHERE id = 3 AND descripcion IS NULL;
UPDATE especialidad SET descripcion = 'Patologías óseas, articulares, fracturas, columna y traumatología deportiva.' WHERE id = 4 AND descripcion IS NULL;
UPDATE especialidad SET descripcion = 'Salud reproductiva, control prenatal, PAP, colposcopía y chequeos ginecológicos.' WHERE id = 5 AND descripcion IS NULL;
UPDATE especialidad SET descripcion = 'Agudeza visual, prescripción de lentes, fondo de ojos y presión ocular.' WHERE id = 6 AND descripcion IS NULL;
UPDATE especialidad SET descripcion = 'Odontología preventiva, operatoria dental, extracciones y endodoncia.' WHERE id = 7 AND descripcion IS NULL;
UPDATE especialidad SET descripcion = 'Ecografías, radiología digital, doppler y estudios diagnósticos.' WHERE id = 8 AND descripcion IS NULL;
