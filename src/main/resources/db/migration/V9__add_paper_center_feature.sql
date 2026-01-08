-- Add paper center management feature
-- All new fields are nullable for backward compatibility with existing data
-- Create paper_centers table
CREATE TABLE paper_centers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Create index on name for faster lookups
CREATE INDEX idx_paper_centers_name ON paper_centers (name);
-- Add new columns to users table (all nullable)
ALTER TABLE users
ADD COLUMN secondary_phone_number VARCHAR(20),
ADD COLUMN paper_writing_mode VARCHAR(20),
ADD COLUMN paper_center VARCHAR(255),
ADD COLUMN study_medium VARCHAR(20);
-- Add check constraints for enum values (optional, for data integrity)
ALTER TABLE users
ADD CONSTRAINT check_paper_writing_mode 
CHECK (paper_writing_mode IS NULL OR paper_writing_mode IN ('ONLINE', 'PHYSICAL', 'NOT_WRITING'));
ALTER TABLE users
ADD CONSTRAINT check_study_medium 
CHECK (study_medium IS NULL OR study_medium IN ('SINHALA', 'TAMIL', 'ENGLISH'));
-- Create trigger to automatically update updated_at for paper_centers
CREATE TRIGGER update_paper_centers_updated_at
    BEFORE UPDATE ON paper_centers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
-- Insert some default paper centers for testing/demo (optional)
INSERT INTO paper_centers (name) VALUES
('Colombo Main Center'),
('Kandy Central Hall'),
('Galle Examination Center')
ON CONFLICT (name) DO NOTHING;
