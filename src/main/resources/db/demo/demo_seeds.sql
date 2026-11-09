-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Script Opcional de Semillas de Datos para Desarrollo y QA Local
-- NOTA: Este script NUNCA debe ejecutarse en entornos productivos.
-- =============================================================================

-- Para cargar datos demo en entorno de desarrollo local con Docker o MySQL:
-- mysql -u esu_app -p db_esu < demo_seeds.sql

-- Activar cuenta de testing de desarrollo
UPDATE usuario SET estado_activo = true WHERE email = 'dev@esu.gob.ar';
