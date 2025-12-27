-- Create email_reset_tokens table for storing email reset (email change verification) tokens
-- Similar security model to password_reset_tokens: store only a hash of the secret, never the raw token.

CREATE TABLE email_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    old_email VARCHAR(255) NOT NULL,
    new_email VARCHAR(255) NOT NULL,

    -- Deterministic lookup key (safe to embed in email link)
    token_id UUID NOT NULL,

    -- BCrypt hash of the token secret
    token_hash VARCHAR(255) NOT NULL,

    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_email_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- One active token per user (partial unique index)
CREATE UNIQUE INDEX ux_email_reset_tokens_active_user
    ON email_reset_tokens(user_id)
    WHERE used = FALSE;

-- Lookup efficiency
CREATE UNIQUE INDEX ux_email_reset_tokens_token_id
    ON email_reset_tokens(token_id);

CREATE INDEX idx_email_reset_tokens_user_id
    ON email_reset_tokens(user_id);

-- Query efficiency for active tokens
CREATE INDEX idx_email_reset_tokens_active
    ON email_reset_tokens(user_id, expires_at)
    WHERE used = FALSE;
