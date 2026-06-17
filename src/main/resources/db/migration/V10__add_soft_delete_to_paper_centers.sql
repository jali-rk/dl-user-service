-- Add soft delete support to paper_centers table
ALTER TABLE paper_centers 
ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

-- Create index for filtering deleted records
CREATE INDEX idx_paper_centers_deleted_at ON paper_centers(deleted_at);