-- Remove the bootstrap admin inserted by V1 migration.
-- This keeps legacy migrations immutable while preventing default credentials from persisting.
DELETE FROM users
WHERE email = 'admin@flowboard.com'
  AND username = 'admin_user'
  AND password_hash = '$2a$12$5STUhPFY0gaDU4Ld/SBIa.pARhYCMpJ7.dq/E0iL0NlhITF0CuEaC';
