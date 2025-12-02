-- Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    whatsapp_number VARCHAR(20),
    school VARCHAR(255),
    address TEXT,
    role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'ADMIN', 'MAIN_ADMIN')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED')),
    code_number VARCHAR(50),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Create unique index on email (case-insensitive)
CREATE UNIQUE INDEX idx_users_email_unique ON users (LOWER(email)) WHERE deleted_at IS NULL;

-- Create unique index on code_number (only for students)
CREATE UNIQUE INDEX idx_users_code_number_unique ON users (code_number) WHERE code_number IS NOT NULL AND deleted_at IS NULL;

-- Create index on whatsapp_number for faster lookups
CREATE INDEX idx_users_whatsapp_number ON users (whatsapp_number) WHERE deleted_at IS NULL;

-- Create index on role for filtering
CREATE INDEX idx_users_role ON users (role) WHERE deleted_at IS NULL;

-- Create index on status for filtering
CREATE INDEX idx_users_status ON users (status) WHERE deleted_at IS NULL;

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

