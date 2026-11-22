-- DEVELOPMENT ONLY. Gives the contact API a user (id 1) to work with before login exists.
-- The hash is a placeholder, not a real password. This user is removed once security is added.
USE contact_db;
INSERT IGNORE INTO users (name, email, password_hash)
VALUES ('Dev User', 'dev@example.com', 'placeholder-not-a-real-hash');
