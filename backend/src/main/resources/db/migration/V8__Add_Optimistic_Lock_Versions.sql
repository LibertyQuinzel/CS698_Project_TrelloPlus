-- Add optimistic locking support for concurrent approvals/changes.
ALTER TABLE changes
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE change_approval_requests
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE change_approval_responses
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE approval_requests_summary
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE approval_responses_summary
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
