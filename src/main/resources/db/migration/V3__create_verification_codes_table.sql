-- Create verification_codes table for storing verification codes
CREATE TABLE verification_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('REGISTRATION', 'EMAIL_CHANGE')),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consumed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_verification_codes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index on user_id for faster lookups
CREATE INDEX idx_verification_codes_user_id ON verification_codes(user_id);

-- Create index on type for filtering
CREATE INDEX idx_verification_codes_type ON verification_codes(type);

-- Create index for finding active codes (not consumed and not expired)
CREATE INDEX idx_verification_codes_active ON verification_codes(user_id, type, expires_at, consumed_at)
    WHERE consumed_at IS NULL;

