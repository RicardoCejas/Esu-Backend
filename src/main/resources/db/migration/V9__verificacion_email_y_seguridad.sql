-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V9: Seguridad, Tokens de Verificación y Resguardo de Datos Existentes
-- =============================================================================

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS fecha_expiracion_verificacion DATETIME;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS token_verificacion_usado BOOLEAN DEFAULT FALSE;

-- Marcar preventivamente usuarios activos preexistentes como verificados para evitar bloqueos
UPDATE usuario SET email_verificado = TRUE WHERE estado_activo = TRUE AND (email_verificado IS NULL OR email_verificado = FALSE);

-- Desactivar cuenta desarrollador insegura en esquemas productivos
UPDATE usuario SET estado_activo = FALSE WHERE email = 'dev@esu.gob.ar';
