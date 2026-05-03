CREATE UNIQUE INDEX IF NOT EXISTS idx_users_active_email_unique
ON users (LOWER(email))
WHERE is_deletion_marked IS NULL OR is_deletion_marked = false;
