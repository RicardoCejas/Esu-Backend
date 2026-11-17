-- =============================================================================
-- Migración V11: Esquema canónico de entidades clínicas, farmacológicas,
-- de agendas y de notificaciones para garantizar Flyway como fuente de verdad.
-- =============================================================================

-- 1. Catálogo CIE-10
CREATE TABLE IF NOT EXISTS cie10 (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descripcion VARCHAR(500) NOT NULL,
    capitulo VARCHAR(200) NULL,
    INDEX idx_cie10_codigo (codigo),
    INDEX idx_cie10_descripcion (descripcion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Convenios con Obras Sociales
CREATE TABLE IF NOT EXISTS convenio_obra_social (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_obra_social VARCHAR(255) NOT NULL,
    sigla VARCHAR(255) NOT NULL,
    codigo_rnosc VARCHAR(255) NULL,
    patron_regex_afiliado VARCHAR(255) NULL,
    requiere_coseguro BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Vínculos Familiares
CREATE TABLE IF NOT EXISTS vinculo_familiar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titular_id BIGINT NOT NULL,
    familiar_nombre VARCHAR(255) NOT NULL,
    familiar_apellido VARCHAR(255) NOT NULL,
    familiar_dni VARCHAR(255) NOT NULL,
    tipo_vinculo VARCHAR(255) NOT NULL,
    obra_social VARCHAR(255) NULL,
    nro_afiliado VARCHAR(255) NULL,
    fecha_creacion DATETIME NOT NULL,
    INDEX idx_vinculo_titular (titular_id),
    INDEX idx_vinculo_familiar_dni (familiar_dni),
    CONSTRAINT fk_vinculo_titular FOREIGN KEY (titular_id) REFERENCES paciente(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Plantillas de Evolución Médica
CREATE TABLE IF NOT EXISTS plantilla_evolucion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    especialidad_id BIGINT NULL,
    estructura_anamnesis TEXT NULL,
    estructura_examen_fisico TEXT NULL,
    tratamiento_sugerido TEXT NULL,
    es_publica BOOLEAN DEFAULT TRUE,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion DATETIME NULL,
    CONSTRAINT fk_plantilla_especialidad FOREIGN KEY (especialidad_id) REFERENCES especialidad(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Evolución Médica
CREATE TABLE IF NOT EXISTS evolucion_medica (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    turno_id BIGINT NULL,
    historia_clinica_id BIGINT NULL,
    motivo_consulta TEXT NOT NULL,
    anamnesis TEXT NULL,
    examen_fisico TEXT NULL,
    diagnostico TEXT NULL,
    codigo_cie10 VARCHAR(20) NULL,
    descripcion_cie10 VARCHAR(500) NULL,
    tratamiento TEXT NULL,
    presion_arterial VARCHAR(30) NULL,
    frecuencia_cardiaca VARCHAR(30) NULL,
    temperatura VARCHAR(20) NULL,
    peso VARCHAR(20) NULL,
    talla VARCHAR(20) NULL,
    imc VARCHAR(20) NULL,
    firmada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_firma DATETIME NULL,
    hash_integridad VARCHAR(64) NULL,
    fecha_creacion DATETIME NULL,
    INDEX idx_evolucion_cie10 (codigo_cie10),
    INDEX idx_evolucion_fecha_creacion (fecha_creacion),
    INDEX idx_evolucion_paciente_id (paciente_id),
    INDEX idx_evolucion_profesional_id (profesional_id),
    CONSTRAINT fk_evolucion_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id),
    CONSTRAINT fk_evolucion_profesional FOREIGN KEY (profesional_id) REFERENCES profesional(id),
    CONSTRAINT fk_evolucion_turno FOREIGN KEY (turno_id) REFERENCES turno(id),
    CONSTRAINT fk_evolucion_hc FOREIGN KEY (historia_clinica_id) REFERENCES historia_clinica(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Adendas a Evolución Médica
CREATE TABLE IF NOT EXISTS evolucion_adenda (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evolucion_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    justificacion_clinica VARCHAR(1000) NOT NULL,
    contenido_adicional TEXT NOT NULL,
    fecha_firma DATETIME NOT NULL,
    hash_integridad VARCHAR(64) NULL,
    CONSTRAINT fk_adenda_evolucion FOREIGN KEY (evolucion_id) REFERENCES evolucion_medica(id),
    CONSTRAINT fk_adenda_profesional FOREIGN KEY (profesional_id) REFERENCES profesional(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Receta Digital Oficial
CREATE TABLE IF NOT EXISTS receta_digital (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    evolucion_id BIGINT NULL,
    fecha_emision DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    diagnostico VARCHAR(500) NULL,
    codigo_cie10 VARCHAR(20) NULL,
    indicaciones_generales TEXT NULL,
    estado_receta VARCHAR(20) DEFAULT 'PENDIENTE',
    fecha_dispensacion DATETIME NULL,
    matricula_farmacia_dispensadora VARCHAR(50) NULL,
    hash_integridad VARCHAR(64) NULL,
    codigo_qr_url VARCHAR(500) NULL,
    motivo_anulacion VARCHAR(300) NULL,
    fecha_anulacion DATETIME NULL,
    fecha_creacion DATETIME NULL,
    CONSTRAINT fk_receta_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id),
    CONSTRAINT fk_receta_profesional FOREIGN KEY (profesional_id) REFERENCES profesional(id),
    CONSTRAINT fk_receta_evolucion FOREIGN KEY (evolucion_id) REFERENCES evolucion_medica(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Medicamentos por Renglón de Receta
CREATE TABLE IF NOT EXISTS medicamento_receta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receta_id BIGINT NOT NULL,
    nombre_comercial VARCHAR(255) NULL,
    droga VARCHAR(255) NOT NULL,
    presentacion VARCHAR(255) NULL,
    dosis_indicada VARCHAR(255) NULL,
    posologia VARCHAR(255) NULL,
    via_administracion VARCHAR(255) NULL,
    duracion VARCHAR(255) NULL,
    cantidad VARCHAR(255) NULL,
    CONSTRAINT fk_med_receta FOREIGN KEY (receta_id) REFERENCES receta_digital(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Recordatorio de Turno
CREATE TABLE IF NOT EXISTS recordatorio_turno (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    turno_id BIGINT NOT NULL,
    disponible_desde DATETIME NOT NULL,
    leido BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_recordatorio_turno UNIQUE (turno_id),
    CONSTRAINT fk_recordatorio_turno FOREIGN KEY (turno_id) REFERENCES turno(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Notificaciones de Usuario
CREATE TABLE IF NOT EXISTS notificacion_usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensaje VARCHAR(1000) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion DATETIME NOT NULL,
    link_accion VARCHAR(255) NULL,
    INDEX idx_notif_usuario_fecha (usuario_id, fecha_creacion),
    INDEX idx_notif_usuario_leida (usuario_id, leida),
    CONSTRAINT fk_notif_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Encuesta de Satisfacción Post-Consulta
CREATE TABLE IF NOT EXISTS encuesta_satisfaccion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    turno_id BIGINT NOT NULL UNIQUE,
    paciente_id BIGINT NOT NULL,
    profesional_id BIGINT NOT NULL,
    puntuacion INT NOT NULL,
    tiempo_espera_percibido VARCHAR(50) NULL,
    comentario VARCHAR(1000) NULL,
    fecha_creacion DATETIME NOT NULL,
    INDEX idx_encuesta_turno (turno_id),
    INDEX idx_encuesta_profesional (profesional_id),
    INDEX idx_encuesta_fecha (fecha_creacion),
    CONSTRAINT fk_encuesta_turno FOREIGN KEY (turno_id) REFERENCES turno(id),
    CONSTRAINT fk_encuesta_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id),
    CONSTRAINT fk_encuesta_profesional FOREIGN KEY (profesional_id) REFERENCES profesional(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
