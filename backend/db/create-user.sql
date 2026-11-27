-- Creates the database account the application logs in with (run once, as a MySQL admin such as root).
-- BEFORE running: replace CHANGE_ME with a strong password of your own, and use the same value for the
-- DB_PASSWORD environment variable. Never commit your real password to Git.
CREATE DATABASE IF NOT EXISTS contact_db;

CREATE USER IF NOT EXISTS 'contact_user'@'localhost' IDENTIFIED BY 'CHANGE_ME';

-- the application only gets rights on its own database, not on the whole MySQL server
GRANT ALL PRIVILEGES ON contact_db.* TO 'contact_user'@'localhost';

FLUSH PRIVILEGES;
