-- Mot de passe oublié : adresse email de récupération + table de tokens de réinitialisation.

-- Adresse email réelle (distincte du pseudo stocké dans users.email).
ALTER TABLE users ADD COLUMN IF NOT EXISTS recovery_email VARCHAR(255) UNIQUE;

-- Tokens opaques de réinitialisation (calqués sur refresh_tokens).
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(512) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL
);
