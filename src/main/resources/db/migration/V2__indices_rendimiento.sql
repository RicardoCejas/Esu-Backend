-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V2: Índices de Rendimiento para Búsquedas Críticas (HU-85)
-- =============================================================================

-- Índice en Pacientes para búsqueda rápida por DNI y ordenamiento por apellido
CREATE INDEX idx_paciente_dni ON paciente (dni);
CREATE INDEX idx_paciente_apellido_nombre ON paciente (apellido, nombre);
CREATE INDEX idx_paciente_nro_afiliado ON paciente (nro_afiliado);

-- Índice en Profesionales por matrícula y estado activo
CREATE INDEX idx_profesional_matricula ON profesional (matricula);
CREATE INDEX idx_profesional_estado_activo ON profesional (estado_activo);

-- Índices en Turnos para consultas de agenda médica y filtros por fecha/estado
CREATE INDEX idx_turno_fecha_hora ON turno (fecha_hora);
CREATE INDEX idx_turno_estado ON turno (estado);
CREATE INDEX idx_turno_paciente_id ON turno (paciente_id);
CREATE INDEX idx_turno_profesional_id ON turno (profesional_id);
CREATE INDEX idx_turno_profesional_fecha ON turno (profesional_id, fecha_hora);

-- Índices en Historia Clínica para acceso rápido a antecedentes del paciente
CREATE INDEX idx_hc_paciente_id ON historia_clinica (paciente_id);
CREATE INDEX idx_hc_fecha_atencion ON historia_clinica (fecha_atencion);

-- Índice en Usuarios para login veloz por email
CREATE INDEX idx_usuario_email ON usuario (email);
CREATE INDEX idx_usuario_rol ON usuario (rol);
