-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V8: Cuenta Desarrollador Multi-rol para Testing (HU-DEV)
-- =============================================================================

INSERT INTO usuario (email, password, rol, estado_activo, fecha_alta)
VALUES ('dev@esu.gob.ar', '$2a$10$eDkZ5FpG9zPq5F2uJ4hQ.eW5eS3Q1.K7Gz1o3rY3qN8w7p3o8bS.a', 'ADMIN', true, CURRENT_DATE)
ON DUPLICATE KEY UPDATE estado_activo = true;
