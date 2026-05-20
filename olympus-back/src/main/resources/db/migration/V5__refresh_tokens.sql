-- Table des refresh tokens opaques (rotation + révocation)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id                 BIGSERIAL PRIMARY KEY,
    token              VARCHAR(512) NOT NULL UNIQUE,
    user_id            BIGINT NOT NULL REFERENCES users(id),
    expires_at         TIMESTAMP NOT NULL,
    revoked            BOOLEAN NOT NULL DEFAULT false,
    replaced_by_token  VARCHAR(512),
    created_at         TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens (user_id);
