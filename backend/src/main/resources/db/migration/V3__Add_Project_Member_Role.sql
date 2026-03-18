ALTER TABLE project_members
ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'viewer';

ALTER TABLE project_members
DROP CONSTRAINT IF EXISTS valid_project_member_role;

ALTER TABLE project_members
ADD CONSTRAINT valid_project_member_role CHECK (role IN ('owner', 'editor', 'viewer'));

UPDATE project_members pm
SET role = 'owner'
FROM projects p
WHERE pm.project_id = p.id
  AND pm.user_id = p.owner_id;
