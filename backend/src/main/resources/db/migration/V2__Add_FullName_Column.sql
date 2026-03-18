-- Add fullName column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);

-- Update default admin user with full name
UPDATE users SET full_name = 'Admin User' WHERE email = 'admin@flowboard.com' AND full_name IS NULL;
