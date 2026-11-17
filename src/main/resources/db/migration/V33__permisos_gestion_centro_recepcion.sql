-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migracion V33: Permisos de gestion del centro para cuentas RECEPCIONISTA
-- =============================================================================

INSERT IGNORE INTO usuario_permiso (usuario_id, permiso_id)
SELECT u.id, p.id
FROM usuario u
CROSS JOIN permiso p
WHERE u.rol = 'RECEPCIONISTA'
  AND p.codigo IN ('CONSULTORIOS_ADMINISTRAR', 'REPORTES_VER', 'USUARIOS_ADMINISTRAR');
