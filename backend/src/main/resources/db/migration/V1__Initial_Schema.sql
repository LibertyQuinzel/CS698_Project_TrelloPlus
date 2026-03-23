-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deletion_marked BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP,
    CONSTRAINT valid_email CHECK (email ~ '^[^@]+@[^@]+\.[^@]+$'),
    CONSTRAINT valid_role CHECK (role IN ('ADMIN', 'MANAGER', 'MEMBER', 'VIEWER')),
    CONSTRAINT valid_username CHECK (length(username) >= 3 AND length(username) <= 50)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_username ON users(username);

-- Create projects table
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deletion_marked BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE INDEX idx_projects_owner_id ON projects(owner_id);
CREATE INDEX idx_projects_created_at ON projects(created_at);

-- Create project_members join table
CREATE TABLE IF NOT EXISTS project_members (
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create boards table
CREATE TABLE IF NOT EXISTS boards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    project_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deletion_marked BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_boards_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_boards_project_id ON boards(project_id);

-- Create stages table
CREATE TABLE IF NOT EXISTS stages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    color VARCHAR(50),
    position INTEGER NOT NULL,
    board_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deletion_marked BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_stages_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE
);

CREATE INDEX idx_stages_board_id ON stages(board_id);

-- Create cards table
CREATE TABLE IF NOT EXISTS cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    position INTEGER NOT NULL,
    stage_id UUID NOT NULL,
    assignee_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deletion_marked BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_cards_stage FOREIGN KEY (stage_id) REFERENCES stages(id) ON DELETE CASCADE,
    CONSTRAINT fk_cards_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT valid_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_cards_stage_id ON cards(stage_id);
CREATE INDEX idx_cards_assignee_id ON cards(assignee_id);

-- Insert a default admin user (password: admin123)
INSERT INTO users (email, password_hash, username, role)
VALUES (
    'admin@flowboard.com',
    '$2a$12$5STUhPFY0gaDU4Ld/SBIa.pARhYCMpJ7.dq/E0iL0NlhITF0CuEaC',
    'admin_user',
    'ADMIN'
) ON CONFLICT(email) DO NOTHING;
