-- Add per-item approval status for generated summary artifacts
ALTER TABLE action_items
ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE action_items
DROP CONSTRAINT IF EXISTS valid_action_item_approval_status;

ALTER TABLE action_items
ADD CONSTRAINT valid_action_item_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED'));

ALTER TABLE decisions
ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE decisions
DROP CONSTRAINT IF EXISTS valid_decision_approval_status;

ALTER TABLE decisions
ADD CONSTRAINT valid_decision_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED'));
