-- =============================================================================
-- ESU: Ecosistema de Salud Unificado (Cruz del Eje)
-- Migración V5: unificación de roles y credenciales semilla (Issue #119)
-- =============================================================================
-- Roles válidos del sistema: PACIENTE, PROFESIONAL, RECEPCIONISTA, ADMIN.
-- Los roles legacy MEDICO/USUARIO y el prefijo ROLE_ se normalizan para que las
-- reglas de SecurityConfig y los @PreAuthorize coincidan.
--
-- Credenciales de desarrollo (solo para entornos locales/demo):
--   admin@esu.salud.gob.ar                / Admin-Esu-2026#Cruz
--   dr.martinez@hospitalcrespo.gob.ar     / Martinez-Med-2026#1
--   dra.gonzalez@sanatoriomariaisabel.com / Gonzalez-Med-2026#2
--   dr.cabrera@clinicacabrera.com.ar      / Cabrera-Med-2026#3
--   recepcion@hospitalcrespo.gob.ar       / Recepcion-Esu-2026#4
--   paciente.demo@gmail.com               / Paciente-Demo-2026#5
--   maria.lopez@cruzdeleje.com            / Lopez-Pac-2026#6
-- =============================================================================

UPDATE usuario SET rol = UPPER(TRIM(rol)) WHERE rol IS NOT NULL;

UPDATE usuario SET rol = SUBSTRING(rol, 6) WHERE rol LIKE 'ROLE_%';

UPDATE usuario SET rol = 'PROFESIONAL' WHERE rol IN ('MEDICO', 'DOCTOR');

UPDATE usuario SET rol = 'PACIENTE'
WHERE rol IS NULL OR rol = '' OR rol = 'USUARIO';

UPDATE usuario SET estado_activo = TRUE WHERE estado_activo IS NULL;

UPDATE usuario SET password = '$2a$10$CZTEuLO9FxT1op0.3VK9t.9XA6zavr6cqLmlOr7PweKnu0Jux40I2'
WHERE email = 'admin@esu.salud.gob.ar';

UPDATE usuario SET password = '$2a$10$zgC9XIckVfKo2V9tuWMR1eFTC/.uBzd9z5osiq.03qR304EhHvA4K'
WHERE email = 'dr.martinez@hospitalcrespo.gob.ar';

UPDATE usuario SET password = '$2a$10$66nM.TycqnhpL4vy9nY6Y.1Ln1gWUVCdE.2sWQxGLbGUH4GfN9MiO'
WHERE email = 'dra.gonzalez@sanatoriomariaisabel.com';

UPDATE usuario SET password = '$2a$10$bX47vksRf8IeGf0I8.6gS.ducu4zyM86N/ridV3KSk/B7hQ3fMe2y'
WHERE email = 'dr.cabrera@clinicacabrera.com.ar';

UPDATE usuario SET password = '$2a$10$lk44ZC6ICQqdH9nIFLEOTOsgQxcow8YvsWM692jzgmz.RUyVmJh1q'
WHERE email = 'recepcion@hospitalcrespo.gob.ar';

UPDATE usuario SET password = '$2a$10$Xo1w7j4SxqEVBv8AqoAfV.Yb8/8eQUnfBH2i73nuv9/d9QlY01N.u'
WHERE email = 'paciente.demo@gmail.com';

UPDATE usuario SET password = '$2a$10$aAPk2OVQXlgwMJ7KYp/vYOPNqFdEkTrGkHQ4PQl2VWJcvgPKF4pDq'
WHERE email = 'maria.lopez@cruzdeleje.com';
