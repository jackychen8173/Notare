CREATE TABLE sage_conversations (
    id UUID PRIMARY KEY,
    tutor_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_sage_conversations_tutor_id ON sage_conversations(tutor_id);

CREATE TABLE sage_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES sage_conversations(id),
    role VARCHAR(20) NOT NULL,
    content JSONB NOT NULL,
    pending_action JSONB,
    action_status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_sage_messages_conversation_id ON sage_messages(conversation_id);
