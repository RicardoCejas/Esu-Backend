-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V9: Seguridad, Tokens de Verificación y Resguardo de Datos Existentes
-- =============================================================================

ALTER TABLE usuario ADD COLUMN email_verificado BOOLEAN DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN token_verificacion VARCHAR(255) NULL;
ALTER TABLE usuario ADD COLUMN fecha_expiracion_verificacion DATETIME NULL;
ALTER TABLE usuario ADD COLUMN token_verificacion_usado BOOLEAN DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN fecha_actualizacion_password DATETIME NULL;
ALTER TABLE usuario ADD COLUMN avisos_email BOOLEAN DEFAULT TRUE;
ALTER TABLE usuario ADD COLUMN alto_contraste BOOLEAN DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN tamano_fuente VARCHAR(20) DEFAULT 'normal';

-- Marcar preventivamente usuarios activos preexistentes como verificados para evitar bloqueos
UPDATE usuario SET email_verificado = TRUE WHERE estado_activo = TRUE AND (email_verificado IS NULL OR email_verificado = FALSE);

-- Desactivar cuenta desarrollador insegura en esquemas productivos
UPDATE usuario SET estado_activo = FALSE WHERE email = 'dev@esu.gob.ar';
