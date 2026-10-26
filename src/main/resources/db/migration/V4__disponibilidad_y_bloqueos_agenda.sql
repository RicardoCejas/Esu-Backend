-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V4: Grilla Horaria, Disponibilidad y Bloqueos de Agenda (HU-BE-89 / HU-BE-90)
-- =============================================================================

CREATE TABLE IF NOT EXISTS disponibilidad_profesional (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    profesional_id BIGINT NOT NULL,
    dia_semana VARCHAR(20) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    intervalo_minutos INT NOT NULL,
    centro_salud_id BIGINT,
    activo BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_disp_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_disp_profesional_id ON disponibilidad_profesional (profesional_id);
CREATE INDEX idx_disp_prof_dia ON disponibilidad_profesional (profesional_id, dia_semana);

CREATE TABLE IF NOT EXISTS bloqueo_agenda (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    profesional_id BIGINT NOT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin DATETIME NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    fecha_creacion DATETIME,
    CONSTRAINT fk_bloqueo_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_bloqueo_profesional_id ON bloqueo_agenda (profesional_id);
CREATE INDEX idx_bloqueo_rango ON bloqueo_agenda (profesional_id, fecha_inicio, fecha_fin);
