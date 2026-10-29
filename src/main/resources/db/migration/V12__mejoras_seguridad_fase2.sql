-- Migración V12: Campos de control de intentos para tokens de verificación documental y ampliación de eventos de auditoría
ALTER TABLE token_verificacion_documento 
    ADD COLUMN IF NOT EXISTS intentos INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_intentos INT NOT NULL DEFAULT 5;

ALTER TABLE log_auditoria_seguridad 
    MODIFY COLUMN evento VARCHAR(50) NOT NULL;
