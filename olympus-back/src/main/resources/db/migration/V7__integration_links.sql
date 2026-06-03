-- Liens d'intégration permanents : permettent à une application tierce de confiance
-- (ex. Chiron) d'accéder en lecture aux données nutritionnelles d'un utilisateur Olympus.
-- Le token est opaque et sans expiration : il n'est invalidé que par une révocation explicite.
CREATE TABLE IF NOT EXISTS integration_links (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(512) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    client      VARCHAR(40) NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_integration_links_user_id ON integration_links (user_id);
