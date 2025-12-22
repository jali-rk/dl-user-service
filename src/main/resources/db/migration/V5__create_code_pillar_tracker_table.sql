-- Create table to track the last issued sequential number for each code sub-pillar
-- Sub-pillars range from 110000 to 990000 (81 sub-pillars total)
-- Each sub-pillar can issue codes from XXX001 to XXX999 (9999 codes per sub-pillar)

CREATE TABLE code_pillar_tracker (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sub_pillar_base INTEGER NOT NULL UNIQUE, -- e.g., 560000
    last_issued_number INTEGER NOT NULL,      -- e.g., 560009
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sub_pillar_base CHECK (sub_pillar_base >= 110000 AND sub_pillar_base <= 990000),
    CONSTRAINT chk_last_issued_number CHECK (last_issued_number >= sub_pillar_base AND last_issued_number < sub_pillar_base + 10000)
);

-- Create index for fast lookup by sub_pillar_base
CREATE INDEX idx_code_pillar_tracker_sub_pillar_base ON code_pillar_tracker(sub_pillar_base);

-- Add comment to table
COMMENT ON TABLE code_pillar_tracker IS 'Tracks the last issued sequential number for each code sub-pillar to ensure unique, non-repeating student codes';

