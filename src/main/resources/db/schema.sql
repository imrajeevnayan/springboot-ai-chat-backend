CREATE TABLE users (
    id UUID PRIMARY KEY,
    external_user_id VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_conversations_user_updated ON conversations (user_id, updated_at DESC);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    model VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id)
);

CREATE INDEX idx_messages_conversation_created ON messages (conversation_id, created_at ASC);
