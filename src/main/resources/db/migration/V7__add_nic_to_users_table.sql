-- Add NIC (National Identity Card) column to users table
-- NIC is required for students during registration.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS nic VARCHAR(12);

-- Enforce uniqueness (national id should not be duplicated)
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_nic ON users (nic);

