-- V20: Soporte seguro de autenticación Google OAuth2 / OIDC
ALTER TABLE usuario ADD COLUMN google_sub VARCHAR(64) NULL;
ALTER TABLE usuario ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

CREATE UNIQUE INDEX idx_usuario_google_sub ON usuario(google_sub);
