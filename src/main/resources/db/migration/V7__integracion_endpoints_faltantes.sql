-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V7: integracion de endpoints faltantes y autenticacion avanzada
-- Issue #121.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Catalogo de centros de salud
-- -----------------------------------------------------------------------------
CREATE TABLE centro_salud (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    tipo VARCHAR(255) NOT NULL,
    direccion VARCHAR(255),
    telefono VARCHAR(255),
    ciudad VARCHAR(255),
    horario_atencion VARCHAR(255),
    disponible_guardia BOOLEAN,
    INDEX idx_centro_salud_nombre (nombre),
    INDEX idx_centro_salud_tipo (tipo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO centro_salud (id, nombre, tipo, direccion, telefono, ciudad, horario_atencion, disponible_guardia) VALUES
(1, 'Hospital Regional Dr. Ramon Carrillo', 'Hospital', 'Av. San Martin 1200', '03549-422111', 'Cruz del Eje', 'Guardia 24 horas', TRUE),
(2, 'Hospital Aurelio Crespo', 'Hospital', 'Gral. Paz 450', '03549-423456', 'Cruz del Eje', 'Lunes a viernes 07:00 a 19:00', TRUE),
(3, 'Sanatorio Maria Isabel', 'Sanatorio', 'Belgrano 780', '03549-424567', 'Cruz del Eje', 'Lunes a sabado 08:00 a 20:00', FALSE),
(4, 'Clinica Cabrera', 'Clinica', 'Rivadavia 315', '03549-425678', 'Cruz del Eje', 'Lunes a viernes 08:00 a 18:00', FALSE),
(5, 'Centro de Salud Norte', 'Centro de Salud', 'Los Andes 89', '03549-426789', 'Cruz del Eje', 'Lunes a viernes 07:00 a 15:00', FALSE);

-- -----------------------------------------------------------------------------
-- 2. Catalogo de obras sociales
-- -----------------------------------------------------------------------------
CREATE TABLE obra_social (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(255) NOT NULL UNIQUE,
    nombre VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL,
    INDEX idx_obra_social_codigo (codigo),
    INDEX idx_obra_social_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO obra_social (id, codigo, nombre, activo) VALUES
(1, 'APROSS', 'Apross', TRUE),
(2, 'PAMI', 'PAMI', TRUE),
(3, 'OSDE', 'OSDE', TRUE),
(4, 'SWISS', 'Swiss Medical', TRUE),
(5, 'PARTICULAR', 'Particular', TRUE);

-- -----------------------------------------------------------------------------
-- 3. Datos de agenda y centros por profesional
-- -----------------------------------------------------------------------------
ALTER TABLE profesional ADD COLUMN dias_atencion VARCHAR(255) NULL;
ALTER TABLE profesional ADD COLUMN duracion_turno_min INT NULL DEFAULT 20;

CREATE TABLE profesional_centro_salud (
    profesional_id BIGINT NOT NULL,
    centro_salud_id BIGINT NOT NULL,
    PRIMARY KEY (profesional_id, centro_salud_id),
    CONSTRAINT fk_pcs_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE CASCADE,
    CONSTRAINT fk_pcs_centro FOREIGN KEY (centro_salud_id) REFERENCES centro_salud (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE profesional
SET dias_atencion = 'Lunes,Martes,Miercoles,Jueves,Viernes',
    duracion_turno_min = 20
WHERE id IN (1, 2, 3);

INSERT INTO profesional_centro_salud (profesional_id, centro_salud_id) VALUES
(1, 1),
(1, 4),
(2, 2),
(2, 3),
(3, 4),
(3, 5);

-- -----------------------------------------------------------------------------
-- 4. Datos operativos del turno
-- -----------------------------------------------------------------------------
ALTER TABLE turno ADD COLUMN codigo_verificacion VARCHAR(255) NULL;
ALTER TABLE turno ADD COLUMN obra_social VARCHAR(255) NULL;
ALTER TABLE turno ADD COLUMN numero_afiliado VARCHAR(255) NULL;
ALTER TABLE turno ADD COLUMN copago_monto DECIMAL(10, 2) NULL;
ALTER TABLE turno ADD COLUMN numero_bono VARCHAR(255) NULL;
ALTER TABLE turno ADD COLUMN es_primera_vez BOOLEAN NULL DEFAULT FALSE;
ALTER TABLE turno ADD COLUMN consultorio_numero VARCHAR(255) NULL;
ALTER TABLE turno ADD COLUMN planta VARCHAR(255) NULL;
ALTER TABLE turno ADD COLUMN justificativo_cancelacion TEXT NULL;
ALTER TABLE turno ADD COLUMN hora_check_in VARCHAR(5) NULL;
ALTER TABLE turno ADD COLUMN hora_llamado VARCHAR(5) NULL;
ALTER TABLE turno ADD COLUMN centro_salud_id BIGINT NULL;
ALTER TABLE turno ADD CONSTRAINT fk_turno_centro_salud
    FOREIGN KEY (centro_salud_id) REFERENCES centro_salud (id) ON DELETE SET NULL;

UPDATE turno
SET codigo_verificacion = CONCAT('ESU-', LPAD(id, 6, '0'))
WHERE codigo_verificacion IS NULL;

CREATE UNIQUE INDEX uk_turno_codigo_verificacion ON turno (codigo_verificacion);
CREATE INDEX idx_turno_centro_salud ON turno (centro_salud_id);

-- -----------------------------------------------------------------------------
-- 5. Autenticacion avanzada: 2FA, verificacion de email y reset de password
-- -----------------------------------------------------------------------------
ALTER TABLE usuario ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN two_factor_secret VARCHAR(255) NULL;
ALTER TABLE usuario ADD COLUMN two_factor_backup_codes TEXT NULL;
ALTER TABLE usuario ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN email_verification_token VARCHAR(255) NULL;
ALTER TABLE usuario ADD COLUMN reset_password_token VARCHAR(255) NULL;
ALTER TABLE usuario ADD COLUMN reset_password_expiry DATETIME NULL;

UPDATE usuario SET email_verificado = TRUE;

CREATE INDEX idx_usuario_reset_password_token ON usuario (reset_password_token);
CREATE INDEX idx_usuario_email_verification_token ON usuario (email_verification_token);
