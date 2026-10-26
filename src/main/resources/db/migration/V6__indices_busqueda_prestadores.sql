-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V6: Índices Compuestos para Búsqueda Optimizada de Prestadores (HU-BE-92)
-- =============================================================================

CREATE INDEX idx_disp_centro_activo_prof ON disponibilidad_profesional (centro_salud_id, activo, profesional_id);
CREATE INDEX idx_turno_disponible_fecha ON turno (profesional_id, estado, fecha_hora);
CREATE INDEX idx_pe_especialidad_prof ON profesional_especialidad (especialidad_id, profesional_id);
