-- Create login_history table for recording successful logins (called by BFF after credential validation)

CREATE TABLE login_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    ip_address VARCHAR(255),
    user_agent TEXT,
    logged_in_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_login_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_login_history_user_logged_in_at
    ON login_history(user_id, logged_in_at DESC);
