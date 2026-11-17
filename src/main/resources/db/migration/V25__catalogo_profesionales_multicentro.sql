-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V25: Catálogo Completo de 28 Especialidades, 24 Profesionales Multicentro y Grilla de Agendas (Fase 4)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Catálogo Canónico Completo de 28 Especialidades de Consultorio
-- -----------------------------------------------------------------------------
INSERT INTO especialidad (nombre, categoria, nombre_canonico) VALUES
('Pediatría', 'CONSULTA', 'Pediatría'),
('Clínica Médica', 'CONSULTA', 'Clínica Médica'),
('Traumatología', 'CONSULTA', 'Traumatología'),
('Ginecología y Obstetricia', 'CONSULTA', 'Ginecología y Obstetricia'),
('Psiquiatría', 'CONSULTA', 'Psiquiatría'),
('Urología', 'CONSULTA', 'Urología'),
('Neurología', 'CONSULTA', 'Neurología'),
('Cardiología', 'CONSULTA', 'Cardiología'),
('Hemoterapia', 'CONSULTA', 'Hemoterapia'),
('Fisiatría', 'CONSULTA', 'Fisiatría'),
('Oftalmología', 'CONSULTA', 'Oftalmología'),
('Diabetología', 'CONSULTA', 'Diabetología'),
('Cirugía General', 'CONSULTA', 'Cirugía General'),
('Psicología', 'CONSULTA', 'Psicología'),
('Odontología', 'CONSULTA', 'Odontología'),
('Trabajo Social', 'CONSULTA', 'Trabajo Social'),
('Nutrición', 'CONSULTA', 'Nutrición'),
('Fonoaudiología', 'CONSULTA', 'Fonoaudiología'),
('Kinesiología', 'CONSULTA', 'Kinesiología'),
('Fisioterapia', 'CONSULTA', 'Fisioterapia'),
('Psicomotricidad', 'CONSULTA', 'Psicomotricidad'),
('Psicopedagogía', 'CONSULTA', 'Psicopedagogía'),
('Dermatología', 'CONSULTA', 'Dermatología'),
('Endocrinología', 'CONSULTA', 'Endocrinología'),
('Flebología', 'CONSULTA', 'Flebología'),
('Gastroenterología', 'CONSULTA', 'Gastroenterología'),
('Oncología', 'CONSULTA', 'Oncología'),
('Otorrinolaringología (ORL)', 'CONSULTA', 'Otorrinolaringología (ORL)')
ON DUPLICATE KEY UPDATE
    categoria = VALUES(categoria),
    nombre_canonico = VALUES(nombre_canonico);

-- -----------------------------------------------------------------------------
-- 2. Cuentas de Acceso de Usuario para los 24 Profesionales Médicos
-- Clave de prueba unificada: Medico123! ($2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a)
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
('lucia.albarracin@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PROFESIONAL', 1, CURRENT_DATE, 1)
ON DUPLICATE KEY UPDATE
    estado_activo = VALUES(estado_activo),
    email_verificado = VALUES(email_verificado);

-- -----------------------------------------------------------------------------
-- 3. Registro de Profesionales y Vinculación con Cuenta de Usuario
-- -----------------------------------------------------------------------------
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
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    apellido = VALUES(apellido),
    telefono = VALUES(telefono),
    estado_activo = VALUES(estado_activo),
    usuario_id = VALUES(usuario_id);

-- -----------------------------------------------------------------------------
-- 4. Asignación de Especialidad Médica a cada Profesional
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
UNION ALL SELECT p.id, e.id FROM profesional p, especialidad e WHERE p.matricula = 'MP 14.390' AND e.nombre_canonico = 'Psicología';

-- -----------------------------------------------------------------------------
-- 5. Vinculaciones Multicentro (Centros 1, 2 y 5; estrictamente NINGUNO en Centro de Diagnóstico 3)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO profesional_centro_salud (profesional_id, centro_salud_id)
-- 1. Dra. María Elena Fernández: Centros 1 y 2
SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 28.451'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 28.451'
-- 2. Dr. Roberto Carlos Martínez: Centros 1 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 19.324'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 19.324'
-- 3. Dra. Florencia Castro: Centros 2 y 5
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 31.220'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 31.220'
-- 4. Dra. Laura Silvina Gómez: Centros 1 y 2
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 33.109'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 33.109'
-- 5. Dr. Esteban Álvarez: Centros 1 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 24.890'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 24.890'
-- 6. Dr. Alejandro Rossi: Centros 1, 2 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 15.670'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 15.670'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 15.670'
-- 7. Dra. Mariana Lucero: Centros 1 y 2
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 27.810'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 27.810'
-- 8. Dra. Valeria Benítez: Centros 1, 2 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 29.544'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 29.544'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 29.544'
-- 9. Dr. Hugo Acosta: Centros 1 y 2
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 18.905'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 18.905'
-- 10. Dra. Silvina Torres: Centros 1 y 2
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 22.188'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 22.188'
-- 11. Dr. Fernando Molina: Centros 1 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 25.430'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 25.430'
-- 12. Dr. Gustavo Navarro: Centros 1, 2 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 20.401'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 20.401'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 20.401'
-- 13. Dra. Patricia Morales: Centros 1 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 12.873'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 12.873'
-- 14. Dra. Beatriz Suárez: Centros 1, 2 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 26.790'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 26.790'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 26.790'
-- 15. Dr. Claudio Peralta: Centros 1, 2 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 16.920'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 16.920'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 16.920'
-- 16. Dr. Javier Domínguez: Centros 1, 2 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 21.340'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 21.340'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 21.340'
-- 17. Dr. Martín Vargas: Centros 1, 2 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 17.512'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 17.512'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 17.512'
-- 18. Dra. Marcela Ibarra: Centros 1 y 2
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 23.411'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 23.411'
-- 19. Dra. Gabriela Sánchez: Centros 1 y 2
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 28.190'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 28.190'
-- 20. Lic. Gonzalo Romero: Centros 1 y 2
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 9.420'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 9.420'
-- 21. Lic. Romina Paredes: Centros 1, 2 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 11.230'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 11.230'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 11.230'
-- 22. Dr. Andrés Correa: Centros 1 y 2
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 19.880'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 19.880'
-- 23. Dra. Carolina Ramos: Centros 1 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 27.810-B'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 27.810-B'
-- 24. Lic. Lucía Albarracín: Centros 1, 2 y 5
UNION ALL SELECT p.id, 1 FROM profesional p WHERE p.matricula = 'MP 14.390'
UNION ALL SELECT p.id, 2 FROM profesional p WHERE p.matricula = 'MP 14.390'
UNION ALL SELECT p.id, 5 FROM profesional p WHERE p.matricula = 'MP 14.390';

-- -----------------------------------------------------------------------------
-- 6. Grilla de Disponibilidad Horaria Semanal (Intervalo 30 minutos, sin solapamientos entre sedes)
-- -----------------------------------------------------------------------------
INSERT INTO disponibilidad_profesional (profesional_id, centro_salud_id, dia_semana, hora_inicio, hora_fin, intervalo_minutos, activo)
SELECT p.id, c.cid, c.dia, c.inicio, c.fin, 30, 1
FROM (
    -- 1. Dra. María Elena Fernández
    SELECT 'MP 28.451' AS mat, 1 AS cid, 'MONDAY' AS dia, TIME '08:00:00' AS inicio, TIME '12:00:00' AS fin
    UNION ALL SELECT 'MP 28.451', 1, 'WEDNESDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 28.451', 2, 'FRIDAY', TIME '14:00:00', TIME '18:00:00'
    -- 2. Dr. Roberto Carlos Martínez
    UNION ALL SELECT 'MP 19.324', 1, 'TUESDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 19.324', 5, 'THURSDAY', TIME '14:00:00', TIME '18:00:00'
    -- 3. Dra. Florencia Castro
    UNION ALL SELECT 'MP 31.220', 2, 'MONDAY', TIME '09:00:00', TIME '13:00:00'
    UNION ALL SELECT 'MP 31.220', 2, 'TUESDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 31.220', 5, 'THURSDAY', TIME '09:00:00', TIME '13:00:00'
    -- 4. Dra. Laura Silvina Gómez
    UNION ALL SELECT 'MP 33.109', 1, 'MONDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 33.109', 1, 'WEDNESDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 33.109', 2, 'THURSDAY', TIME '08:00:00', TIME '12:00:00'
    -- 5. Dr. Esteban Álvarez
    UNION ALL SELECT 'MP 24.890', 1, 'MONDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 24.890', 5, 'FRIDAY', TIME '09:00:00', TIME '13:00:00'
    -- 6. Dr. Alejandro Rossi
    UNION ALL SELECT 'MP 15.670', 1, 'MONDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 15.670', 2, 'WEDNESDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 15.670', 5, 'FRIDAY', TIME '09:00:00', TIME '13:00:00'
    -- 7. Dra. Mariana Lucero
    UNION ALL SELECT 'MP 27.810', 1, 'TUESDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 27.810', 2, 'FRIDAY', TIME '14:00:00', TIME '18:00:00'
    -- 8. Dra. Valeria Benítez
    UNION ALL SELECT 'MP 29.544', 1, 'TUESDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 29.544', 2, 'FRIDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 29.544', 5, 'WEDNESDAY', TIME '09:00:00', TIME '13:00:00'
    -- 9. Dr. Hugo Acosta
    UNION ALL SELECT 'MP 18.905', 1, 'MONDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 18.905', 2, 'THURSDAY', TIME '08:00:00', TIME '12:00:00'
    -- 10. Dra. Silvina Torres
    UNION ALL SELECT 'MP 22.188', 1, 'MONDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 22.188', 1, 'WEDNESDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 22.188', 2, 'THURSDAY', TIME '14:00:00', TIME '18:00:00'
    -- 11. Dr. Fernando Molina
    UNION ALL SELECT 'MP 25.430', 1, 'TUESDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 25.430', 5, 'FRIDAY', TIME '09:00:00', TIME '13:00:00'
    -- 12. Dr. Gustavo Navarro
    UNION ALL SELECT 'MP 20.401', 1, 'THURSDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 20.401', 2, 'FRIDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 20.401', 5, 'TUESDAY', TIME '09:00:00', TIME '13:00:00'
    -- 13. Dra. Patricia Morales
    UNION ALL SELECT 'MP 12.873', 1, 'MONDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 12.873', 1, 'WEDNESDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 12.873', 5, 'TUESDAY', TIME '14:00:00', TIME '18:00:00'
    -- 14. Dra. Beatriz Suárez
    UNION ALL SELECT 'MP 26.790', 1, 'WEDNESDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 26.790', 2, 'FRIDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 26.790', 5, 'MONDAY', TIME '14:00:00', TIME '18:00:00'
    -- 15. Dr. Claudio Peralta
    UNION ALL SELECT 'MP 16.920', 1, 'MONDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 16.920', 2, 'THURSDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 16.920', 5, 'TUESDAY', TIME '14:00:00', TIME '18:00:00'
    -- 16. Dr. Javier Domínguez
    UNION ALL SELECT 'MP 21.340', 1, 'TUESDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 21.340', 2, 'THURSDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 21.340', 5, 'FRIDAY', TIME '09:00:00', TIME '13:00:00'
    -- 17. Dr. Martín Vargas
    UNION ALL SELECT 'MP 17.512', 1, 'MONDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 17.512', 2, 'WEDNESDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 17.512', 5, 'FRIDAY', TIME '08:00:00', TIME '12:00:00'
    -- 18. Dra. Marcela Ibarra
    UNION ALL SELECT 'MP 23.411', 1, 'TUESDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 23.411', 2, 'FRIDAY', TIME '14:00:00', TIME '18:00:00'
    -- 19. Dra. Gabriela Sánchez
    UNION ALL SELECT 'MP 28.190', 1, 'MONDAY', TIME '09:00:00', TIME '13:00:00'
    UNION ALL SELECT 'MP 28.190', 2, 'THURSDAY', TIME '09:00:00', TIME '13:00:00'
    -- 20. Lic. Gonzalo Romero
    UNION ALL SELECT 'MP 9.420', 1, 'MONDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 9.420', 1, 'WEDNESDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 9.420', 2, 'FRIDAY', TIME '14:00:00', TIME '18:00:00'
    -- 21. Lic. Romina Paredes
    UNION ALL SELECT 'MP 11.230', 1, 'TUESDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 11.230', 2, 'THURSDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 11.230', 5, 'FRIDAY', TIME '14:00:00', TIME '18:00:00'
    -- 22. Dr. Andrés Correa
    UNION ALL SELECT 'MP 19.880', 1, 'WEDNESDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 19.880', 2, 'FRIDAY', TIME '08:00:00', TIME '12:00:00'
    -- 23. Dra. Carolina Ramos
    UNION ALL SELECT 'MP 27.810-B', 1, 'MONDAY', TIME '09:00:00', TIME '13:00:00'
    UNION ALL SELECT 'MP 27.810-B', 5, 'WEDNESDAY', TIME '09:00:00', TIME '13:00:00'
    -- 24. Lic. Lucía Albarracín
    UNION ALL SELECT 'MP 14.390', 1, 'TUESDAY', TIME '14:00:00', TIME '18:00:00'
    UNION ALL SELECT 'MP 14.390', 2, 'WEDNESDAY', TIME '08:00:00', TIME '12:00:00'
    UNION ALL SELECT 'MP 14.390', 5, 'FRIDAY', TIME '14:00:00', TIME '18:00:00'
) c
JOIN profesional p ON p.matricula = c.mat
WHERE NOT EXISTS (
    SELECT 1 FROM disponibilidad_profesional dp
    WHERE dp.profesional_id = p.id
      AND dp.centro_salud_id = c.cid
      AND dp.dia_semana = c.dia
      AND dp.hora_inicio = c.inicio
);
