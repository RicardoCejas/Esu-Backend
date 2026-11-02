-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V24: Organización de Centros, Servicios, Especialidades y Agendas (Fase 4)
-- =============================================================================

-- 1. Ampliar centro_salud con capacidades asistenciales
ALTER TABLE centro_salud ADD COLUMN permite_consultas BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE centro_salud ADD COLUMN permite_estudios BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Actualizar y normalizar los cuatro centros de salud activos de Cruz del Eje
UPDATE centro_salud SET
    nombre = 'Hospital Provincial Dr. Aurelio Crespo',
    tipo = 'HOSPITAL',
    permite_consultas = TRUE,
    permite_estudios = TRUE,
    activo = TRUE
WHERE id = 1;

UPDATE centro_salud SET
    nombre = 'Clínica Cabrera',
    tipo = 'CLINICA',
    permite_consultas = TRUE,
    permite_estudios = TRUE,
    activo = TRUE
WHERE id = 2;

UPDATE centro_salud SET
    nombre = 'Centro de Diagnóstico por Imágenes Cruz del Eje',
    tipo = 'CONSULTORIO_PRIVADO',
    permite_consultas = FALSE,
    permite_estudios = TRUE,
    activo = TRUE
WHERE id = 3;

-- Desactivar dispensarios o centros antiguos para nuevas reservas preservando datos históricos
UPDATE centro_salud SET
    activo = FALSE
WHERE id NOT IN (1, 2, 3, 5);

-- Insertar Sanatorio María Isabel (ID: 5) si no existe
INSERT INTO centro_salud (id, nombre, tipo, direccion, telefono, ciudad, horario_atencion, disponible_guardia, latitud, longitud, permite_consultas, permite_estudios, activo)
VALUES (5, 'Sanatorio María Isabel', 'CLINICA', 'Pellegrini 320, Cruz del Eje', '03549-421800', 'Cruz del Eje', 'Lunes a Viernes 08:00 a 20:00', FALSE, -30.7280, -64.8010, TRUE, FALSE, TRUE)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    permite_consultas = VALUES(permite_consultas),
    permite_estudios = VALUES(permite_estudios),
    activo = VALUES(activo);

-- 3. Ampliar turno con tipo de atención, servicio y especialidad
ALTER TABLE turno ADD COLUMN tipo_atencion VARCHAR(50) NOT NULL DEFAULT 'CONSULTA';
ALTER TABLE turno ADD COLUMN servicio VARCHAR(150) NULL;
ALTER TABLE turno ADD COLUMN especialidad_id BIGINT NULL;

UPDATE turno SET tipo_atencion = 'CONSULTA' WHERE tipo_atencion IS NULL;

-- 4. Ampliar especialidad con categoría y nombre canónico
ALTER TABLE especialidad ADD COLUMN categoria VARCHAR(50) NOT NULL DEFAULT 'CONSULTA';
ALTER TABLE especialidad ADD COLUMN nombre_canonico VARCHAR(180) NULL;

-- Normalización de nombres de especialidades iniciales
UPDATE especialidad SET nombre = 'Clínica Médica', nombre_canonico = 'Clínica Médica', categoria = 'CONSULTA' WHERE id = 1;
UPDATE especialidad SET nombre = 'Pediatría', nombre_canonico = 'Pediatría', categoria = 'CONSULTA' WHERE id = 2;
UPDATE especialidad SET nombre = 'Cardiología', nombre_canonico = 'Cardiología', categoria = 'CONSULTA' WHERE id = 3;
UPDATE especialidad SET nombre = 'Traumatología', nombre_canonico = 'Traumatología', categoria = 'CONSULTA' WHERE id = 4;
UPDATE especialidad SET nombre = 'Ginecología y Obstetricia', nombre_canonico = 'Ginecología y Obstetricia', categoria = 'CONSULTA' WHERE id = 5;
UPDATE especialidad SET nombre = 'Oftalmología', nombre_canonico = 'Oftalmología', categoria = 'CONSULTA' WHERE id = 6;
UPDATE especialidad SET nombre = 'Odontología', nombre_canonico = 'Odontología', categoria = 'CONSULTA' WHERE id = 7;
UPDATE especialidad SET nombre = 'Diagnóstico por Imágenes', nombre_canonico = 'Diagnóstico por Imágenes', categoria = 'SERVICIO' WHERE id = 8;
