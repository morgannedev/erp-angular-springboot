CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Índices para mejorar el rendimiento
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_usuario_id ON refresh_tokens(usuario_id);
CREATE INDEX idx_refresh_tokens_expiry_date ON refresh_tokens(expiry_date);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked);

-- Comentarios de tabla y columnas
COMMENT ON TABLE refresh_tokens IS 'Almacena tokens de refresco para autenticación';
COMMENT ON COLUMN refresh_tokens.token IS 'Token único de refresco';
COMMENT ON COLUMN refresh_tokens.usuario_id IS 'ID del usuario asociado';
COMMENT ON COLUMN refresh_tokens.username IS 'Nombre de usuario para referencia rápida';
COMMENT ON COLUMN refresh_tokens.expiry_date IS 'Fecha de expiración del token';
COMMENT ON COLUMN refresh_tokens.revoked IS 'Indica si el token ha sido revocado';