-- Create meetings table (U2: Meeting Capture)
CREATE TABLE IF NOT EXISTS meetings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    meeting_date DATE NOT NULL,
    meeting_time TIME,
    platform VARCHAR(50),
    meeting_link VARCHAR(500),
    transcript TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_meetings_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_meetings_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT valid_meeting_status CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED')),
    CONSTRAINT project_id_not_null CHECK (project_id IS NOT NULL)
);

CREATE INDEX idx_meetings_project_id ON meetings(project_id);
CREATE INDEX idx_meetings_created_by ON meetings(created_by);
CREATE INDEX idx_meetings_status ON meetings(status);
CREATE INDEX idx_meetings_created_at ON meetings(created_at);

-- Create meeting_members join table (U2: Project-specific meeting members)
CREATE TABLE IF NOT EXISTS meeting_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_meeting_members_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    CONSTRAINT fk_meeting_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_meeting_user UNIQUE (meeting_id, user_id)
);

CREATE INDEX idx_meeting_members_meeting_id ON meeting_members(meeting_id);
CREATE INDEX idx_meeting_members_user_id ON meeting_members(user_id);

-- Create meeting_notes table (U2: Notes capture)
CREATE TABLE IF NOT EXISTS meeting_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_meeting_notes_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE
);

CREATE INDEX idx_meeting_notes_meeting_id ON meeting_notes(meeting_id);

-- Create meeting_summaries table (U2: Summary Generation)
CREATE TABLE IF NOT EXISTS meeting_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id UUID NOT NULL,
    ai_generated_content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    CONSTRAINT fk_meeting_summaries_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    CONSTRAINT valid_summary_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_meeting_summaries_meeting_id ON meeting_summaries(meeting_id);
CREATE INDEX idx_meeting_summaries_status ON meeting_summaries(status);

-- Create action_items table (U2: Extracted from summary)
CREATE TABLE IF NOT EXISTS action_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id UUID NOT NULL,
    description TEXT NOT NULL,
    source_context TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    assigned_to UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_action_items_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    CONSTRAINT fk_action_items_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT valid_action_item_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT valid_action_item_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_action_items_meeting_id ON action_items(meeting_id);
CREATE INDEX idx_action_items_assigned_to ON action_items(assigned_to);
CREATE INDEX idx_action_items_status ON action_items(status);

-- Create decisions table (U2: Extracted from summary)
CREATE TABLE IF NOT EXISTS decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id UUID NOT NULL,
    description TEXT NOT NULL,
    source_context TEXT,
    impact_summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_decisions_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE
);

CREATE INDEX idx_decisions_meeting_id ON decisions(meeting_id);

-- Create changes table (U2/U3 shared: Board changes)
CREATE TABLE IF NOT EXISTS changes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id UUID NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    before_state JSONB,
    after_state JSONB,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_changes_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    CONSTRAINT valid_change_type CHECK (change_type IN ('MOVE_CARD', 'UPDATE_CARD', 'CREATE_CARD', 'DELETE_CARD')),
    CONSTRAINT valid_change_status CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'READY_FOR_APPLICATION', 'APPLIED'))
);

CREATE INDEX idx_changes_meeting_id ON changes(meeting_id);
CREATE INDEX idx_changes_status ON changes(status);

-- Create approval_requests_summary table (U2: Approval workflow)
CREATE TABLE IF NOT EXISTS approval_requests_summary (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id UUID NOT NULL,
    required_approvals INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_approval_requests_summary_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE
);

CREATE INDEX idx_approval_requests_summary_meeting_id ON approval_requests_summary(meeting_id);

-- Create approval_responses_summary table (U2: Per-user approval responses)
CREATE TABLE IF NOT EXISTS approval_responses_summary (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approval_request_id UUID NOT NULL,
    user_id UUID NOT NULL,
    response VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    comments TEXT,
    responded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_approval_responses_summary_request FOREIGN KEY (approval_request_id) REFERENCES approval_requests_summary(id) ON DELETE CASCADE,
    CONSTRAINT fk_approval_responses_summary_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT valid_approval_response CHECK (response IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT unique_user_per_request UNIQUE (approval_request_id, user_id)
);

CREATE INDEX idx_approval_responses_summary_request_id ON approval_responses_summary(approval_request_id);
CREATE INDEX idx_approval_responses_summary_user_id ON approval_responses_summary(user_id);
CREATE INDEX idx_approval_responses_summary_response ON approval_responses_summary(response);
