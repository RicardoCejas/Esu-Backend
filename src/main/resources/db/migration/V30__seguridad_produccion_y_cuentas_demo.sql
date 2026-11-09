-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V30: Endurecimiento de Seguridad en Producción y Neutralización de Semillas Demo (HU-12)
-- =============================================================================

-- 1. Desactivar permanentemente la cuenta técnica de testing 'dev@esu.gob.ar'
UPDATE usuario
SET estado_activo = false
WHERE email = 'dev@esu.gob.ar';

-- 2. Neutralizar contraseñas conocidas de desarrollo en todas las cuentas preexistentes.
-- Se reemplazan los hashes conocidos de desarrollo por tokens bloqueados de alta entropía,
-- obligando a que cualquier acceso legítimo requiera asignación explícita de credenciales seguras.
UPDATE usuario
SET password = CONCAT('$2a$10$LOCKED_PROD_', SUBSTRING(SHA2(CONCAT(UUID(), email, NOW()), 256), 1, 35))
WHERE password = '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a';
