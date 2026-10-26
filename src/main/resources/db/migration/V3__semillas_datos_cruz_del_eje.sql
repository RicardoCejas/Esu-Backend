-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V3: Semillas de Datos Iniciales (Data Seeding) (HU-88)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Especialidades Médicas de Cruz del Eje
-- -----------------------------------------------------------------------------
INSERT INTO especialidad (id, nombre) VALUES
(1, 'Clínica Médica / Medicina General'),
(2, 'Pediatría y Neonatología'),
(3, 'Cardiología Clínica'),
(4, 'Traumatología y Ortopedia'),
(5, 'Ginecología y Obstetricia'),
(6, 'Oftalmología'),
(7, 'Odontología Integral'),
(8, 'Diagnóstico por Imágenes');

-- -----------------------------------------------------------------------------
-- 2. Usuarios del Sistema (Contraseñas de desarrollo)
-- -----------------------------------------------------------------------------
INSERT INTO usuario (id, email, password, rol, estado_activo, fecha_alta) VALUES
(1, 'admin@esu.salud.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'ADMIN', true, '2026-01-10'),
(2, 'dr.martinez@hospitalcrespo.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'MEDICO', true, '2026-01-15'),
(3, 'dra.gonzalez@sanatoriomariaisabel.com', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'MEDICO', true, '2026-02-01'),
(4, 'dr.cabrera@clinicacabrera.com.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'MEDICO', true, '2026-02-10'),
(5, 'recepcion@hospitalcrespo.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'RECEPCIONISTA', true, '2026-02-15'),
(6, 'paciente.demo@gmail.com', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PACIENTE', true, '2026-03-01'),
(7, 'maria.lopez@cruzdeleje.com', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'PACIENTE', true, '2026-03-05');

-- -----------------------------------------------------------------------------
-- 3. Profesionales Médicos de la Red de Salud
-- -----------------------------------------------------------------------------
INSERT INTO profesional (id, nombre, apellido, matricula, telefono, estado_activo, usuario_id) VALUES
(1, 'Carlos', 'Martínez', 'MP-34982', '03549-422111', true, 2),
(2, 'Lucía', 'González', 'MP-28104', '03549-423456', true, 3),
(3, 'Esteban', 'Cabrera', 'MP-19402', '03549-424567', true, 4);

-- -----------------------------------------------------------------------------
-- 4. Asignación de Especialidades a Profesionales
-- -----------------------------------------------------------------------------
INSERT INTO profesional_especialidad (profesional_id, especialidad_id) VALUES
(1, 1), -- Dr. Martínez: Clínica Médica
(1, 3), -- Dr. Martínez: Cardiología
(2, 2), -- Dra. González: Pediatría
(3, 4), -- Dr. Cabrera: Traumatología
(3, 1); -- Dr. Cabrera: Clínica Médica

-- -----------------------------------------------------------------------------
-- 5. Pacientes Registrados
-- -----------------------------------------------------------------------------
INSERT INTO paciente (id, nombre, apellido, dni, nro_afiliado, telefono, fecha_nacimiento, obra_social, grupo_sanguineo, fk_usuario) VALUES
(1, 'Lucas', 'Ramírez', '38123456', 'APO-38123456', '03549-15501234', '1994-05-18', 'Apross', '0+', 6),
(2, 'María', 'López', '40987654', 'PAR-40987654', '03549-15609876', '1998-11-23', 'Particular', 'A+', 7);

-- -----------------------------------------------------------------------------
-- 6. Turnos Médicos Iniciales
-- -----------------------------------------------------------------------------
INSERT INTO turno (id, fecha_hora, estado, motivo_turno, fecha_creacion, paciente_id, profesional_id) VALUES
(1, '2026-10-15 09:00:00', 'CONFIRMADO', 'Control clínico anual y chequeo de presión', NOW(), 1, 1),
(2, '2026-10-15 10:30:00', 'EN_ESPERA', 'Consulta por dolor lumbar persistente', NOW(), 2, 3),
(3, '2026-10-16 11:00:00', 'CONFIRMADO', 'Control de niño sano y vacunas', NOW(), 1, 2);

-- -----------------------------------------------------------------------------
-- 7. Historia Clínica de Prueba
-- -----------------------------------------------------------------------------
INSERT INTO historia_clinica (id, diagnostico, motivo_consulta, tratamiento, fecha_atencion, paciente_id, profesional_id) VALUES
(1, 'Hipertensión arterial leve estadio 1', 'Chequeo rutinario por cefalea matinal', 'Dieta hiposódica, actividad física regular y control tensional en 30 días.', '2026-09-10 09:30:00', 1, 1);
