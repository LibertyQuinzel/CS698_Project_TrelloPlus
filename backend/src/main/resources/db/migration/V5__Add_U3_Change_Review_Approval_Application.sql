-- U3: Extend changes lifecycle status values
ALTER TABLE changes
DROP CONSTRAINT IF EXISTS valid_change_status;

ALTER TABLE changes
ADD CONSTRAINT valid_change_status CHECK (
    status IN (
        'PENDING',
        'UNDER_REVIEW',
        'APPROVED',
        'REJECTED',
        'READY_FOR_APPLICATION',
        'APPLYING',
        'APPLIED',
        'ROLLED_BACK',
        'ARCHIVED'
    )
);

-- U3: Track application metadata directly on changes
ALTER TABLE changes
ADD COLUMN IF NOT EXISTS applied_by UUID,
ADD COLUMN IF NOT EXISTS applied_at TIMESTAMP;

ALTER TABLE changes
ADD CONSTRAINT fk_changes_applied_by
FOREIGN KEY (applied_by) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_changes_applied_by ON changes(applied_by);
CREATE INDEX IF NOT EXISTS idx_changes_applied_at ON changes(applied_at);

-- U3: Per-change approval request
CREATE TABLE IF NOT EXISTS change_approval_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    change_id UUID NOT NULL UNIQUE,
    required_approvals INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_change_approval_requests_change FOREIGN KEY (change_id) REFERENCES changes(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_change_approval_requests_change_id ON change_approval_requests(change_id);

-- U3: Per-user decision records
CREATE TABLE IF NOT EXISTS change_approval_responses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approval_request_id UUID NOT NULL,
    user_id UUID NOT NULL,
    decision VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    feedback TEXT,
    decided_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_change_approval_responses_request FOREIGN KEY (approval_request_id) REFERENCES change_approval_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_change_approval_responses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT valid_change_approval_decision CHECK (decision IN ('PENDING', 'APPROVE', 'REJECT', 'DEFER')),
    CONSTRAINT unique_change_decision_per_user UNIQUE (approval_request_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_change_approval_responses_request_id ON change_approval_responses(approval_request_id);
CREATE INDEX IF NOT EXISTS idx_change_approval_responses_user_id ON change_approval_responses(user_id);
CREATE INDEX IF NOT EXISTS idx_change_approval_responses_decision ON change_approval_responses(decision);

-- U3: Snapshot for rollback support
CREATE TABLE IF NOT EXISTS change_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    change_id UUID NOT NULL,
    board_id UUID NOT NULL,
    board_state JSONB NOT NULL,
    rollback_state JSONB,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    applied_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_change_snapshots_change FOREIGN KEY (change_id) REFERENCES changes(id) ON DELETE CASCADE,
    CONSTRAINT fk_change_snapshots_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    CONSTRAINT valid_snapshot_verification_status CHECK (verification_status IN ('PENDING', 'VERIFIED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_change_snapshots_change_id ON change_snapshots(change_id);
CREATE INDEX IF NOT EXISTS idx_change_snapshots_board_id ON change_snapshots(board_id);

-- U3: Immutable audit trail
CREATE TABLE IF NOT EXISTS change_audit_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    change_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    actor_id UUID,
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_change_audit_entries_change FOREIGN KEY (change_id) REFERENCES changes(id) ON DELETE CASCADE,
    CONSTRAINT fk_change_audit_entries_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_change_audit_entries_change_id ON change_audit_entries(change_id);
CREATE INDEX IF NOT EXISTS idx_change_audit_entries_actor_id ON change_audit_entries(actor_id);
CREATE INDEX IF NOT EXISTS idx_change_audit_entries_action ON change_audit_entries(action);
