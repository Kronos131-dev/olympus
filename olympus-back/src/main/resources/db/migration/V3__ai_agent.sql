-- Agent IA conversationnel : préférence de fournisseur + persistance des conversations

ALTER TABLE users ADD COLUMN IF NOT EXISTS ai_provider VARCHAR(20) NOT NULL DEFAULT 'MISTRAL';

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_ai_provider_check;
ALTER TABLE users ADD CONSTRAINT users_ai_provider_check
    CHECK (ai_provider IN ('MISTRAL', 'GEMINI'));

CREATE TABLE IF NOT EXISTS conversations (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    title       VARCHAR(255),
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT,
    created_at      TIMESTAMP NOT NULL,
    CONSTRAINT chat_messages_role_check CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations (user_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation_id ON chat_messages (conversation_id);
