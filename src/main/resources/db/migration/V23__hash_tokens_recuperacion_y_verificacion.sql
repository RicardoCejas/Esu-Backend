-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V23: Hasheo criptográfico SHA-256 de tokens de recuperación y verificación
-- Resguardo contra fugas de tokens en reposo (Ley 25.326)
-- =============================================================================

-- Hashear tokens preexistentes en texto plano en token_recuperacion
UPDATE token_recuperacion
SET token = SHA2(token, 256)
WHERE token IS NOT NULL AND CHAR_LENGTH(token) != 64;

-- Hashear tokens preexistentes en texto plano en usuario (token_verificacion)
UPDATE usuario
SET token_verificacion = SHA2(token_verificacion, 256)
WHERE token_verificacion IS NOT NULL AND CHAR_LENGTH(token_verificacion) != 64;

CREATE INDEX idx_usuario_token_verificacion ON usuario (token_verificacion);
