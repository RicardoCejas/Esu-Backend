-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V38: Ampliación de Especialidades en Hospital Aurelio Crespo,
-- Vinculaciones Multicentro y Disponibilidad de Turnos hasta el 1 de Diciembre
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Normalización de Centros de Salud de Cruz del Eje
-- -----------------------------------------------------------------------------
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
    tipo = 'CENTRO_DIAGNOSTICO',
    permite_consultas = FALSE,
    permite_estudios = TRUE,
    activo = TRUE
WHERE id = 3;

INSERT INTO centro_salud (id, nombre, tipo, direccion, telefono, ciudad, horario_atencion, disponible_guardia, latitud, longitud, permite_consultas, permite_estudios, activo)
VALUES (5, 'Sanatorio María Isabel', 'SANATORIO_PRIVADO', 'Pellegrini 320, Cruz del Eje', '03549-421800', 'Cruz del Eje', 'Lunes a Viernes 08:00 a 20:00', FALSE, -30.7280, -64.8010, TRUE, FALSE, TRUE)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    tipo = VALUES(tipo),
    permite_consultas = VALUES(permite_consultas),
    permite_estudios = VALUES(permite_estudios),
    activo = VALUES(activo);

-- -----------------------------------------------------------------------------
-- 2. Catálogo Canónico Completo de 28 Especialidades
-- -----------------------------------------------------------------------------
UPDATE especialidad SET nombre = 'Clínica Médica', nombre_canonico = 'Clínica Médica', categoria = 'CONSULTA'
WHERE id = 1 AND NOT EXISTS (SELECT 1 FROM (SELECT id FROM especialidad WHERE nombre = 'Clínica Médica' AND id <> 1 LIMIT 1) AS dup);

UPDATE especialidad SET nombre = 'Pediatría', nombre_canonico = 'Pediatría', categoria = 'CONSULTA'
WHERE id = 2 AND NOT EXISTS (SELECT 1 FROM (SELECT id FROM especialidad WHERE nombre = 'Pediatría' AND id <> 2 LIMIT 1) AS dup);

UPDATE especialidad SET nombre = 'Cardiología', nombre_canonico = 'Cardiología', categoria = 'CONSULTA'
WHERE id = 3 AND NOT EXISTS (SELECT 1 FROM (SELECT id FROM especialidad WHERE nombre = 'Cardiología' AND id <> 3 LIMIT 1) AS dup);

UPDATE especialidad SET nombre = 'Traumatología', nombre_canonico = 'Traumatología', categoria = 'CONSULTA'
WHERE id = 4 AND NOT EXISTS (SELECT 1 FROM (SELECT id FROM especialidad WHERE nombre = 'Traumatología' AND id <> 4 LIMIT 1) AS dup);

UPDATE especialidad SET nombre = 'Ginecología y Obstetricia', nombre_canonico = 'Ginecología y Obstetricia', categoria = 'CONSULTA'
WHERE id = 5 AND NOT EXISTS (SELECT 1 FROM (SELECT id FROM especialidad WHERE nombre = 'Ginecología y Obstetricia' AND id <> 5 LIMIT 1) AS dup);

UPDATE especialidad SET nombre = 'Oftalmología', nombre_canonico = 'Oftalmología', categoria = 'CONSULTA'
WHERE id = 6 AND NOT EXISTS (SELECT 1 FROM (SELECT id FROM especialidad WHERE nombre = 'Oftalmología' AND id <> 6 LIMIT 1) AS dup);

UPDATE especialidad SET nombre = 'Odontología', nombre_canonico = 'Odontología', categoria = 'CONSULTA'
WHERE id = 7 AND NOT EXISTS (SELECT 1 FROM (SELECT id FROM especialidad WHERE nombre = 'Odontología' AND id <> 7 LIMIT 1) AS dup);

UPDATE especialidad SET nombre = 'Diagnóstico por Imágenes', nombre_canonico = 'Diagnóstico por Imágenes', categoria = 'SERVICIO'
WHERE id = 8 AND NOT EXISTS (SELECT 1 FROM (SELECT id FROM especialidad WHERE nombre = 'Diagnóstico por Imágenes' AND id <> 8 LIMIT 1) AS dup);

INSERT INTO especialidad (nombre, categoria, nombre_canonico, descripcion) VALUES
('Diagnóstico por Imágenes', 'SERVICIO', 'Diagnóstico por Imágenes', 'Ecografías, radiología digital, doppler y estudios diagnósticos.')
ON DUPLICATE KEY UPDATE
    categoria = VALUES(categoria),
    nombre_canonico = VALUES(nombre_canonico),
    descripcion = VALUES(descripcion);

INSERT INTO especialidad (nombre, categoria, nombre_canonico, descripcion) VALUES
('Pediatría', 'CONSULTA', 'Pediatría', 'Atención integral del niño y recién nacido.'),
('Clínica Médica', 'CONSULTA', 'Clínica Médica', 'Atención médica primaria y preventiva en adultos.'),
('Traumatología', 'CONSULTA', 'Traumatología', 'Diagnóstico y tratamiento de lesiones óseas y articulares.'),
('Ginecología y Obstetricia', 'CONSULTA', 'Ginecología y Obstetricia', 'Salud integral de la mujer y control del embarazo.'),
('Psiquiatría', 'CONSULTA', 'Psiquiatría', 'Salud mental y tratamientos psicofarmacológicos.'),
('Urología', 'CONSULTA', 'Urología', 'Afecciones del sistema urinario y reproductor masculino.'),
('Neurología', 'CONSULTA', 'Neurología', 'Trastornos del sistema nervioso central y periférico.'),
('Cardiología', 'CONSULTA', 'Cardiología', 'Enfermedades cardíacas y prevención cardiovascular.'),
('Hemoterapia', 'CONSULTA', 'Hemoterapia', 'Medicina transfusional y banco de sangre.'),
('Fisiatría', 'CONSULTA', 'Fisiatría', 'Medicina física y rehabilitación musculoesquelética.'),
('Oftalmología', 'CONSULTA', 'Oftalmología', 'Salud visual, control de agudeza y fondo de ojo.'),
('Diabetología', 'CONSULTA', 'Diabetología', 'Tratamiento de diabetes y control metabólico integral.'),
('Cirugía General', 'CONSULTA', 'Cirugía General', 'Procedimientos quirúrgicos y evaluaciones preoperatorias.'),
('Psicología', 'CONSULTA', 'Psicología', 'Atención psicológica clínica y psicoterapia.'),
('Odontología', 'CONSULTA', 'Odontología', 'Salud bucal, extracciones, limpieza y tratamientos dentales.'),
('Trabajo Social', 'CONSULTA', 'Trabajo Social', 'Acompañamiento social y orientación sociosanitaria.'),
('Nutrición', 'CONSULTA', 'Nutrición', 'Planes de alimentación, control de peso y educación nutricional.'),
('Fonoaudiología', 'CONSULTA', 'Fonoaudiología', 'Trastornos de la voz, lenguaje, audición y deglución.'),
('Kinesiología', 'CONSULTA', 'Kinesiología', 'Rehabilitación motora, respiratoria y fisiocinética.'),
('Fisioterapia', 'CONSULTA', 'Fisioterapia', 'Terapias físicas para dolor articular y muscular.'),
('Psicomotricidad', 'CONSULTA', 'Psicomotricidad', 'Desarrollo corporal y motriz infantil y adulto.'),
('Psicopedagogía', 'CONSULTA', 'Psicopedagogía', 'Diagnóstico e intervención en procesos de aprendizaje.'),
('Dermatología', 'CONSULTA', 'Dermatología', 'Cuidado y patologías de la piel, uñas y cabello.'),
('Endocrinología', 'CONSULTA', 'Endocrinología', 'Trastornos hormonales, tiroides y metabolismo.'),
('Flebología', 'CONSULTA', 'Flebología', 'Tratamiento de várices y circulación venosa.'),
('Gastroenterología', 'CONSULTA', 'Gastroenterología', 'Aparato digestivo, estómago, hígado e intestinos.'),
('Oncología', 'CONSULTA', 'Oncología', 'Diagnóstico y seguimiento oncológico integral.'),
('Otorrinolaringología (ORL)', 'CONSULTA', 'Otorrinolaringología (ORL)', 'Oído, nariz y garganta.')
ON DUPLICATE KEY UPDATE
    categoria = VALUES(categoria),
    nombre_canonico = VALUES(nombre_canonico),
    descripcion = VALUES(descripcion);

-- -----------------------------------------------------------------------------
-- 3. Cuentas de Acceso y Profesionales Médicos Especialistas
-- Clave unificada de prueba: Medico123! ($2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a)
-- -----------------------------------------------------------------------------
INSERT INTO usuario (email, password, rol, estado_activo, fecha_alta, email_verificado) VALUES
('maria.fernandez@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('roberto.martinez@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('florencia.castro@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('laura.gomez@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('esteban.alvarez@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('alejandro.rossi@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('mariana.lucero@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('valeria.benitez@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('hugo.acosta@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('silvina.torres@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('fernando.molina@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('gustavo.navarro@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('patricia.morales@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('beatriz.suarez@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('claudio.peralta@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('javier.dominguez@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('martin.vargas@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('marcela.ibarra@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('gabriela.sanchez@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('gonzalo.romero@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('romina.paredes@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('andres.correa@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('carolina.ramos@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('lucia.albarracin@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('nicolas.heredia@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('marcos.valenzuela@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('claudia.soria@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('juancruz.carrizo@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('daniela.maidana@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('sofia.pereyra@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('pablo.godoy@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('camila.aguero@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('martina.bustos@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1),
('sergio.luna@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1)
ON DUPLICATE KEY UPDATE
    estado_activo = VALUES(estado_activo),
    email_verificado = VALUES(email_verificado);

INSERT INTO profesional (matricula, nombre, apellido, telefono, estado_activo, usuario_id)
SELECT 'MP 28.451', 'María Elena', 'Fernández', '03549-422111', 1, u.id FROM usuario u WHERE u.email = 'maria.fernandez@esu.salud.gob.ar'
UNION ALL SELECT 'MP 19.324', 'Roberto Carlos', 'Martínez', '03549-422112', 1, u.id FROM usuario u WHERE u.email = 'roberto.martinez@esu.salud.gob.ar'
UNION ALL SELECT 'MP 31.220', 'Florencia', 'Castro', '03549-422113', 1, u.id FROM usuario u WHERE u.email = 'florencia.castro@esu.salud.gob.ar'
UNION ALL SELECT 'MP 33.109', 'Laura Silvina', 'Gómez', '03549-422114', 1, u.id FROM usuario u WHERE u.email = 'laura.gomez@esu.salud.gob.ar'
UNION ALL SELECT 'MP 24.890', 'Esteban', 'Álvarez', '03549-422115', 1, u.id FROM usuario u WHERE u.email = 'esteban.alvarez@esu.salud.gob.ar'
UNION ALL SELECT 'MP 15.670', 'Alejandro', 'Rossi', '03549-422116', 1, u.id FROM usuario u WHERE u.email = 'alejandro.rossi@esu.salud.gob.ar'
UNION ALL SELECT 'MP 27.810', 'Mariana', 'Lucero', '03549-422117', 1, u.id FROM usuario u WHERE u.email = 'mariana.lucero@esu.salud.gob.ar'
UNION ALL SELECT 'MP 29.544', 'Valeria', 'Benítez', '03549-422118', 1, u.id FROM usuario u WHERE u.email = 'valeria.benitez@esu.salud.gob.ar'
UNION ALL SELECT 'MP 18.905', 'Hugo', 'Acosta', '03549-422119', 1, u.id FROM usuario u WHERE u.email = 'hugo.acosta@esu.salud.gob.ar'
UNION ALL SELECT 'MP 22.188', 'Silvina', 'Torres', '03549-422120', 1, u.id FROM usuario u WHERE u.email = 'silvina.torres@esu.salud.gob.ar'
UNION ALL SELECT 'MP 25.430', 'Fernando', 'Molina', '03549-422121', 1, u.id FROM usuario u WHERE u.email = 'fernando.molina@esu.salud.gob.ar'
UNION ALL SELECT 'MP 20.401', 'Gustavo', 'Navarro', '03549-422122', 1, u.id FROM usuario u WHERE u.email = 'gustavo.navarro@esu.salud.gob.ar'
UNION ALL SELECT 'MP 12.873', 'Patricia', 'Morales', '03549-422123', 1, u.id FROM usuario u WHERE u.email = 'patricia.morales@esu.salud.gob.ar'
UNION ALL SELECT 'MP 26.790', 'Beatriz', 'Suárez', '03549-422124', 1, u.id FROM usuario u WHERE u.email = 'beatriz.suarez@esu.salud.gob.ar'
UNION ALL SELECT 'MP 16.920', 'Claudio', 'Peralta', '03549-422125', 1, u.id FROM usuario u WHERE u.email = 'claudio.peralta@esu.salud.gob.ar'
UNION ALL SELECT 'MP 21.340', 'Javier', 'Domínguez', '03549-422126', 1, u.id FROM usuario u WHERE u.email = 'javier.dominguez@esu.salud.gob.ar'
UNION ALL SELECT 'MP 17.512', 'Martín', 'Vargas', '03549-422127', 1, u.id FROM usuario u WHERE u.email = 'martin.vargas@esu.salud.gob.ar'
UNION ALL SELECT 'MP 23.411', 'Marcela', 'Ibarra', '03549-422128', 1, u.id FROM usuario u WHERE u.email = 'marcela.ibarra@esu.salud.gob.ar'
UNION ALL SELECT 'MP 28.190', 'Gabriela', 'Sánchez', '03549-422129', 1, u.id FROM usuario u WHERE u.email = 'gabriela.sanchez@esu.salud.gob.ar'
UNION ALL SELECT 'MP 9.420', 'Gonzalo', 'Romero', '03549-422130', 1, u.id FROM usuario u WHERE u.email = 'gonzalo.romero@esu.salud.gob.ar'
UNION ALL SELECT 'MP 11.230', 'Romina', 'Paredes', '03549-422131', 1, u.id FROM usuario u WHERE u.email = 'romina.paredes@esu.salud.gob.ar'
UNION ALL SELECT 'MP 19.880', 'Andrés', 'Correa', '03549-422132', 1, u.id FROM usuario u WHERE u.email = 'andres.correa@esu.salud.gob.ar'
UNION ALL SELECT 'MP 27.810-B', 'Carolina', 'Ramos', '03549-422133', 1, u.id FROM usuario u WHERE u.email = 'carolina.ramos@esu.salud.gob.ar'
UNION ALL SELECT 'MP 14.390', 'Lucía', 'Albarracín', '03549-422134', 1, u.id FROM usuario u WHERE u.email = 'lucia.albarracin@esu.salud.gob.ar'
UNION ALL SELECT 'MP 30.101', 'Nicolás', 'Heredia', '03549-422135', 1, u.id FROM usuario u WHERE u.email = 'nicolas.heredia@esu.salud.gob.ar'
UNION ALL SELECT 'MP 30.102', 'Marcos', 'Valenzuela', '03549-422136', 1, u.id FROM usuario u WHERE u.email = 'marcos.valenzuela@esu.salud.gob.ar'
UNION ALL SELECT 'MP 30.103', 'Claudia', 'Soria', '03549-422137', 1, u.id FROM usuario u WHERE u.email = 'claudia.soria@esu.salud.gob.ar'
UNION ALL SELECT 'MP 30.104', 'Juan Cruz', 'Carrizo', '03549-422138', 1, u.id FROM usuario u WHERE u.email = 'juancruz.carrizo@esu.salud.gob.ar'
UNION ALL SELECT 'MP 30.105', 'Daniela', 'Maidana', '03549-422139', 1, u.id FROM usuario u WHERE u.email = 'daniela.maidana@esu.salud.gob.ar'
UNION ALL SELECT 'MP 30.106', 'Sofía', 'Pereyra', '03549-422140', 1, u.id FROM usuario u WHERE u.email = 'sofia.pereyra@esu.salud.gob.ar'
UNION ALL SELECT 'MP 30.107', 'Pablo', 'Godoy', '03549-422141', 1, u.id FROM usuario u WHERE u.email = 'pablo.godoy@esu.salud.gob.ar'
UNION ALL SELECT 'MP 30.108', 'Camila', 'Agüero', '03549-422142', 1, u.id FROM usuario u WHERE u.email = 'camila.aguero@esu.salud.gob.ar'
UNION ALL SELECT 'MP 30.109', 'Martina', 'Bustos', '03549-422143', 1, u.id FROM usuario u WHERE u.email = 'martina.bustos@esu.salud.gob.ar'
UNION ALL SELECT 'MP 30.110', 'Sergio', 'Luna', '03549-422144', 1, u.id FROM usuario u WHERE u.email = 'sergio.luna@esu.salud.gob.ar'
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    apellido = VALUES(apellido),
    telefono = VALUES(telefono),
    estado_activo = VALUES(estado_activo);

-- -----------------------------------------------------------------------------
-- 4. Asignación de Especialidades a los Profesionales
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO profesional_especialidad (profesional_id, especialidad_id)
SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 28.451' AND e.nombre_canonico = 'Clínica Médica'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 19.324' AND e.nombre_canonico = 'Clínica Médica'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 31.220' AND e.nombre_canonico = 'Clínica Médica'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 33.109' AND e.nombre_canonico = 'Pediatría'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 24.890' AND e.nombre_canonico = 'Pediatría'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 15.670' AND e.nombre_canonico = 'Cardiología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 27.810' AND e.nombre_canonico = 'Cardiología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 29.544' AND e.nombre_canonico = 'Traumatología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 18.905' AND e.nombre_canonico = 'Traumatología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 22.188' AND e.nombre_canonico = 'Ginecología y Obstetricia'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 25.430' AND e.nombre_canonico = 'Ginecología y Obstetricia'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 20.401' AND e.nombre_canonico = 'Oftalmología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 12.873' AND e.nombre_canonico = 'Odontología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 26.790' AND e.nombre_canonico = 'Dermatología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 16.920' AND e.nombre_canonico = 'Neurología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 21.340' AND e.nombre_canonico = 'Urología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 17.512' AND e.nombre_canonico = 'Cirugía General'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 23.411' AND e.nombre_canonico = 'Gastroenterología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 28.190' AND e.nombre_canonico = 'Endocrinología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 9.420' AND e.nombre_canonico = 'Kinesiología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 11.230' AND e.nombre_canonico = 'Nutrición'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 19.880' AND e.nombre_canonico = 'Otorrinolaringología (ORL)'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 27.810-B' AND e.nombre_canonico = 'Psiquiatría'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 14.390' AND e.nombre_canonico = 'Psicología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 30.101' AND e.nombre_canonico = 'Diabetología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 30.102' AND e.nombre_canonico = 'Flebología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 30.103' AND e.nombre_canonico = 'Hemoterapia'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 30.104' AND e.nombre_canonico = 'Fisiatría'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 30.105' AND e.nombre_canonico = 'Trabajo Social'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 30.106' AND e.nombre_canonico = 'Fonoaudiología'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 30.107' AND e.nombre_canonico = 'Fisioterapia'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 30.108' AND e.nombre_canonico = 'Psicomotricidad'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 30.109' AND e.nombre_canonico = 'Psicopedagogía'
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 30.110' AND e.nombre_canonico = 'Oncología';

-- -----------------------------------------------------------------------------
-- 5. Vinculaciones Multicentro: Aurelio Crespo (TODAS), Cabrera e Isabel
-- -----------------------------------------------------------------------------
-- Hospital Aurelio Crespo (ID 1): Asignar TODOS los profesionales activos para habilitar las 28 especialidades
INSERT IGNORE INTO profesional_centro_salud (profesional_id, centro_salud_id)
SELECT id, 1 FROM profesional WHERE estado_activo = 1;

-- Clínica Cabrera (ID 2): 16 especialidades ambulatorias
INSERT IGNORE INTO profesional_centro_salud (profesional_id, centro_salud_id)
SELECT p.id, 2 FROM profesional p
WHERE p.matricula IN (
    'MP 28.451', 'MP 31.220', 'MP 33.109', 'MP 15.670', 'MP 27.810',
    'MP 29.544', 'MP 18.905', 'MP 22.188', 'MP 20.401', 'MP 26.790',
    'MP 16.920', 'MP 21.340', 'MP 17.512', 'MP 23.411', 'MP 28.190',
    'MP 9.420', 'MP 11.230', 'MP 19.880', 'MP 14.390', 'MP 30.101',
    'MP 30.102', 'MP 30.110'
);

-- Sanatorio María Isabel (ID 5): 14 especialidades clínicas y quirúrgicas
INSERT IGNORE INTO profesional_centro_salud (profesional_id, centro_salud_id)
SELECT p.id, 5 FROM profesional p
WHERE p.matricula IN (
    'MP 19.324', 'MP 31.220', 'MP 24.890', 'MP 15.670', 'MP 29.544',
    'MP 25.430', 'MP 20.401', 'MP 12.873', 'MP 26.790', 'MP 16.920',
    'MP 21.340', 'MP 17.512', 'MP 11.230', 'MP 27.810-B', 'MP 14.390',
    'MP 30.110'
);

-- -----------------------------------------------------------------------------
-- 6. Grilla de Disponibilidad Horaria Semanal (Intervalo 30 min)
-- -----------------------------------------------------------------------------
INSERT INTO disponibilidad_profesional (profesional_id, centro_salud_id, dia_semana, hora_inicio, hora_fin, intervalo_minutos, activo)
SELECT p.id, pcs.centro_salud_id, d.dia, d.inicio, d.fin, 30, 1
FROM profesional p
JOIN profesional_centro_salud pcs ON pcs.profesional_id = p.id
CROSS JOIN (
    SELECT 'MONDAY' AS dia, TIME '08:00:00' AS inicio, TIME '12:00:00' AS fin
    UNION ALL SELECT 'TUESDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'WEDNESDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'THURSDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'FRIDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MONDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'TUESDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'WEDNESDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'THURSDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'FRIDAY', TIME '14:00:00', TIME '18:00:00'
) d
WHERE NOT EXISTS (
    SELECT 1 FROM disponibilidad_profesional dp
    WHERE dp.profesional_id = p.id
      AND dp.centro_salud_id = pcs.centro_salud_id
      AND dp.dia_semana = d.dia
      AND dp.hora_inicio = d.inicio
);

-- -----------------------------------------------------------------------------
-- 7. Turnos Médicos Disponibles desde HOY (14-Nov-2026) hasta el 01-Dic-2026
-- -----------------------------------------------------------------------------
INSERT INTO turno (fecha_hora, estado, motivo_turno, fecha_creacion, profesional_id, centro_salud_id, centro_salud_nombre, tipo_atencion, especialidad_id)
SELECT 
    TIMESTAMP(d.fecha, h.hora) AS fecha_hora,
    'DISPONIBLE' AS estado,
    'Consulta médica programada' AS motivo_turno,
    NOW() AS fecha_creacion,
    p.id AS profesional_id,
    c.id AS centro_salud_id,
    c.nombre AS centro_salud_nombre,
    'CONSULTA' AS tipo_atencion,
    pe.especialidad_id AS especialidad_id
FROM 
    (
        SELECT DATE '2026-11-14' AS fecha UNION ALL
        SELECT DATE '2026-11-16' UNION ALL
        SELECT DATE '2026-11-17' UNION ALL
        SELECT DATE '2026-11-18' UNION ALL
        SELECT DATE '2026-11-19' UNION ALL
        SELECT DATE '2026-11-20' UNION ALL
        SELECT DATE '2026-11-21' UNION ALL
        SELECT DATE '2026-11-23' UNION ALL
        SELECT DATE '2026-11-24' UNION ALL
        SELECT DATE '2026-11-25' UNION ALL
        SELECT DATE '2026-11-26' UNION ALL
        SELECT DATE '2026-11-27' UNION ALL
        SELECT DATE '2026-11-28' UNION ALL
        SELECT DATE '2026-11-30' UNION ALL
        SELECT DATE '2026-12-01'
    ) d
    CROSS JOIN (
        SELECT TIME '08:30:00' AS hora UNION ALL
        SELECT TIME '09:30:00' UNION ALL
        SELECT TIME '10:30:00' UNION ALL
        SELECT TIME '11:30:00' UNION ALL
        SELECT TIME '14:30:00' UNION ALL
        SELECT TIME '15:30:00' UNION ALL
        SELECT TIME '16:30:00'
    ) h
    JOIN profesional_centro_salud pcs ON 1=1
    JOIN profesional p ON p.id = pcs.profesional_id AND p.estado_activo = 1
    JOIN centro_salud c ON c.id = pcs.centro_salud_id AND c.activo = 1
    JOIN profesional_especialidad pe ON pe.profesional_id = p.id
    WHERE NOT EXISTS (
        SELECT 1 FROM turno t2 
        WHERE t2.profesional_id = p.id 
          AND t2.fecha_hora = TIMESTAMP(d.fecha, h.hora)
    );
