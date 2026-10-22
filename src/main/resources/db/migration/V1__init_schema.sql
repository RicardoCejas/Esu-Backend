-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V1: Esquema Inicial de Base de Datos
-- =============================================================================

CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    estado_activo BOOLEAN DEFAULT TRUE,
    fecha_alta DATE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS especialidad (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS paciente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    dni VARCHAR(20) NOT NULL UNIQUE,
    nro_afiliado VARCHAR(50) NOT NULL UNIQUE,
    telefono VARCHAR(50),
    fecha_nacimiento DATE,
    obra_social VARCHAR(150),
    grupo_sanguineo VARCHAR(10),
    fk_usuario BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_paciente_usuario FOREIGN KEY (fk_usuario) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS profesional (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    matricula VARCHAR(50) NOT NULL UNIQUE,
    telefono VARCHAR(50),
    estado_activo BOOLEAN DEFAULT TRUE,
    usuario_id BIGINT UNIQUE,
    CONSTRAINT fk_profesional_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS profesional_especialidad (
    profesional_id BIGINT NOT NULL,
    especialidad_id BIGINT NOT NULL,
    PRIMARY KEY (profesional_id, especialidad_id),
    CONSTRAINT fk_pe_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE CASCADE,
    CONSTRAINT fk_pe_especialidad FOREIGN KEY (especialidad_id) REFERENCES especialidad (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS turno (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_hora DATETIME NOT NULL,
    estado VARCHAR(50) NOT NULL,
    motivo_turno VARCHAR(255),
    fecha_creacion DATETIME,
    paciente_id BIGINT,
    profesional_id BIGINT,
    CONSTRAINT fk_turno_paciente FOREIGN KEY (paciente_id) REFERENCES paciente (id) ON DELETE SET NULL,
    CONSTRAINT fk_turno_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS historia_clinica (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    diagnostico VARCHAR(255),
    motivo_consulta VARCHAR(255),
    tratamiento VARCHAR(255),
    fecha_atencion DATETIME,
    paciente_id BIGINT,
    profesional_id BIGINT,
    CONSTRAINT fk_hc_paciente FOREIGN KEY (paciente_id) REFERENCES paciente (id) ON DELETE CASCADE,
    CONSTRAINT fk_hc_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
