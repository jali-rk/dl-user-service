-- Add lookup-friendly token_id to password_reset_tokens.
-- We store only a hash of the secret in token_hash, and use token_id for deterministic lookup.

ALTER TABLE password_reset_tokens
    ADD COLUMN token_id UUID;

-- Populate existing rows (if any) with a token_id so the table remains consistent.
UPDATE password_reset_tokens
SET token_id = gen_random_uuid()
WHERE token_id IS NULL;

ALTER TABLE password_reset_tokens
    ALTER COLUMN token_id SET NOT NULL;

-- Enforce uniqueness for token lookup.
CREATE UNIQUE INDEX ux_password_reset_tokens_token_id ON password_reset_tokens(token_id);

