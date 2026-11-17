-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V32: Catalogo de Permisos Granulares y PBAC (HU-INT-06)
-- =============================================================================

CREATE TABLE IF NOT EXISTS permiso (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    modulo VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255) NULL,
    INDEX idx_permiso_modulo (modulo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usuario_permiso (
    usuario_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, permiso_id),
    CONSTRAINT fk_usuperm_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_usuperm_permiso FOREIGN KEY (permiso_id) REFERENCES permiso(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1. Catalogo canonico de permisos granulares
INSERT INTO permiso (codigo, nombre, modulo, descripcion) VALUES
('TURNOS_VER', 'Ver agenda y listados de turnos', 'TURNOS', 'Permite consultar turnos del dia y filtros por profesional o centro'),
('TURNOS_CREAR', 'Otorgar turnos regulares', 'TURNOS', 'Permite agendar turnos estandar de mostrador'),
('TURNOS_CANCELAR', 'Cancelar turnos con motivo', 'TURNOS', 'Permite dar de baja un turno con motivo estructurado'),
('TURNOS_REAGENDAR', 'Reagendar turnos', 'TURNOS', 'Permite modificar horario y fecha de un turno asignado'),
('SOBRETURNOS_CREAR', 'Crear sobreturnos con consultorio', 'TURNOS', 'Permite emitir sobreturnos validando cupo maximo diario'),
('SALA_ESPERA_GESTIONAR', 'Gestionar sala de espera', 'SALA_ESPERA', 'Permite mover pacientes en Kanban y llamar por audio/pantalla'),
('PACIENTES_VER', 'Consultar ficha de pacientes', 'PACIENTES', 'Permite buscar por DNI y ver antecedentes demograficos'),
('PACIENTES_REGISTRAR', 'Alta rapida de pacientes', 'PACIENTES', 'Permite registrar un nuevo paciente desde mostrador'),
('PACIENTES_FUSIONAR', 'Fusionar fichas duplicadas', 'PACIENTES', 'Permite resolver homonimos y unificar historias clinicas'),
('CONSULTORIOS_VER', 'Ver consultorios fisicos', 'CONSULTORIOS', 'Permite visualizar salas y ocupacion fisica'),
('CONSULTORIOS_ADMINISTRAR', 'Administrar consultorios fisicos', 'CONSULTORIOS', 'Permite crear, editar y dar de baja salas y consultorios'),
('MEDICOS_VER', 'Ver nomina de medicos', 'MEDICOS', 'Permite consultar el listado de profesionales y especialidades'),
('MEDICOS_ASIGNAR_CONSULTORIO', 'Asignar consultorio a medicos', 'MEDICOS', 'Permite vincular fisicamente un profesional a una sala'),
('REPORTES_VER', 'Ver metricas y telemetria', 'REPORTES', 'Permite acceder a estadisticas de ausentismo y tiempos de espera'),
('USUARIOS_ADMINISTRAR', 'Gestionar usuarios y permisos', 'USUARIOS', 'Permite dar de alta usuarios y editar sus roles y permisos');

-- 2. Backfill: asignar permisos por defecto a usuarios existentes segun rol actual
INSERT IGNORE INTO usuario_permiso (usuario_id, permiso_id)
SELECT u.id, p.id FROM usuario u CROSS JOIN permiso p WHERE u.rol = 'ADMIN';

INSERT IGNORE INTO usuario_permiso (usuario_id, permiso_id)
SELECT u.id, p.id FROM usuario u CROSS JOIN permiso p 
WHERE u.rol = 'RECEPCIONISTA' AND p.codigo IN (
    'TURNOS_VER', 'TURNOS_CREAR', 'TURNOS_CANCELAR', 'TURNOS_REAGENDAR', 
    'SOBRETURNOS_CREAR', 'SALA_ESPERA_GESTIONAR', 'PACIENTES_VER', 
    'PACIENTES_REGISTRAR', 'CONSULTORIOS_VER', 'MEDICOS_VER', 'MEDICOS_ASIGNAR_CONSULTORIO'
);

INSERT IGNORE INTO usuario_permiso (usuario_id, permiso_id)
SELECT u.id, p.id FROM usuario u CROSS JOIN permiso p 
WHERE u.rol IN ('MEDICO', 'PROFESIONAL') AND p.codigo IN (
    'TURNOS_VER', 'PACIENTES_VER', 'CONSULTORIOS_VER', 'MEDICOS_VER'
);
