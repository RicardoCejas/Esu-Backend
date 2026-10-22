-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V4: Evoluciones, recetas, catálogo CIE-10 y auditoría de accesos
-- Issue #118: completa el esquema para que ddl-auto=validate funcione.
-- Se usa IF NOT EXISTS para no romper bases ya creadas por Hibernate.
-- =============================================================================

CREATE TABLE IF NOT EXISTS cie10 (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descripcion VARCHAR(500) NOT NULL,
    capitulo VARCHAR(200),
    INDEX idx_cie10_codigo (codigo),
    INDEX idx_cie10_descripcion (descripcion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS evolucion_medica (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    turno_id BIGINT NULL,
    historia_clinica_id BIGINT NULL,
    motivo_consulta TEXT NOT NULL,
    anamnesis TEXT,
    examen_fisico TEXT,
    diagnostico TEXT,
    codigo_cie10 VARCHAR(20),
    descripcion_cie10 VARCHAR(500),
    tratamiento TEXT,
    presion_arterial VARCHAR(30),
    frecuencia_cardiaca VARCHAR(30),
    temperatura VARCHAR(20),
    peso VARCHAR(20),
    talla VARCHAR(20),
    imc VARCHAR(20),
    firmada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_firma DATETIME NULL,
    hash_integridad VARCHAR(64),
    fecha_creacion DATETIME NULL,
    CONSTRAINT fk_evo_paciente FOREIGN KEY (paciente_id) REFERENCES paciente (id) ON DELETE CASCADE,
    CONSTRAINT fk_evo_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id),
    CONSTRAINT fk_evo_turno FOREIGN KEY (turno_id) REFERENCES turno (id) ON DELETE SET NULL,
    CONSTRAINT fk_evo_historia FOREIGN KEY (historia_clinica_id) REFERENCES historia_clinica (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS evolucion_adenda (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evolucion_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    justificacion_clinica VARCHAR(1000) NOT NULL,
    contenido_adicional TEXT NOT NULL,
    fecha_firma DATETIME NOT NULL,
    hash_integridad VARCHAR(64),
    CONSTRAINT fk_adenda_evolucion FOREIGN KEY (evolucion_id) REFERENCES evolucion_medica (id) ON DELETE CASCADE,
    CONSTRAINT fk_adenda_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS receta_digital (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    evolucion_id BIGINT NULL,
    fecha_emision DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    diagnostico VARCHAR(500),
    codigo_cie10 VARCHAR(20),
    indicaciones_generales TEXT,
    hash_integridad VARCHAR(64),
    codigo_qr_url VARCHAR(500),
    fecha_creacion DATETIME NULL,
    CONSTRAINT fk_receta_paciente FOREIGN KEY (paciente_id) REFERENCES paciente (id) ON DELETE CASCADE,
    CONSTRAINT fk_receta_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id),
    CONSTRAINT fk_receta_evolucion FOREIGN KEY (evolucion_id) REFERENCES evolucion_medica (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS medicamento_receta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receta_id BIGINT NOT NULL,
    nombre_comercial VARCHAR(255),
    droga VARCHAR(255) NOT NULL,
    presentacion VARCHAR(255),
    dosis_indicada VARCHAR(255),
    posologia VARCHAR(255),
    via_administracion VARCHAR(255),
    duracion VARCHAR(255),
    cantidad VARCHAR(255),
    CONSTRAINT fk_medicamento_receta FOREIGN KEY (receta_id) REFERENCES receta_digital (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS log_acceso_hc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    paciente_id BIGINT NOT NULL,
    fecha_hora DATETIME NOT NULL,
    accion VARCHAR(100) NOT NULL,
    ip_address VARCHAR(50),
    rol_usuario VARCHAR(50),
    INDEX idx_log_paciente (paciente_id),
    INDEX idx_log_usuario (usuario_id),
    INDEX idx_log_fecha (fecha_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
