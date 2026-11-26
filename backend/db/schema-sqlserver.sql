-- Microsoft SQL Server version of schema.sql (schema.sql is the MySQL one, used by default).
-- Run with SSMS or:  sqlcmd -S localhost -E -i schema-sqlserver.sql
-- "GO" is not SQL: it tells those tools to send the lines above as one batch.

IF DB_ID(N'contact_db') IS NULL
    CREATE DATABASE contact_db;
GO

USE contact_db;
GO

-- Differences from MySQL:
--   AUTO_INCREMENT  ->  IDENTITY(1,1)
--   TIMESTAMP       ->  DATETIME2   (in SQL Server "TIMESTAMP" means something completely different)
--   VARCHAR         ->  NVARCHAR    (so names in Urdu and other languages are stored correctly)

CREATE TABLE users (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    name          NVARCHAR(100) NOT NULL,
    email         NVARCHAR(255) NULL,
    phone         NVARCHAR(20)  NULL,
    password_hash NVARCHAR(255) NOT NULL,
    created_at    DATETIME2 DEFAULT SYSDATETIME()
);
GO

-- A normal UNIQUE constraint in SQL Server allows only ONE NULL. A user may have no email (or no phone),
-- so we use "filtered" unique indexes: values must be unique only when they are not NULL.
CREATE UNIQUE INDEX ux_users_email ON users(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX ux_users_phone ON users(phone) WHERE phone IS NOT NULL;
GO

CREATE TABLE contacts (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    first_name NVARCHAR(100) NOT NULL,
    last_name  NVARCHAR(100) NOT NULL,
    title      NVARCHAR(100) NULL,
    deleted_at DATETIME2 NULL,
    created_at DATETIME2 DEFAULT SYSDATETIME(),

    CONSTRAINT fk_contacts_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);
GO

CREATE TABLE emails (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    contact_id BIGINT NOT NULL,
    email      NVARCHAR(255) NOT NULL,
    label      NVARCHAR(30)  NOT NULL,

    CONSTRAINT fk_emails_contact FOREIGN KEY (contact_id)
        REFERENCES contacts(id) ON DELETE CASCADE
);
GO

CREATE TABLE phones (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    contact_id BIGINT NOT NULL,
    phone      NVARCHAR(20) NOT NULL,
    label      NVARCHAR(30) NOT NULL,

    CONSTRAINT fk_phones_contact FOREIGN KEY (contact_id)
        REFERENCES contacts(id) ON DELETE CASCADE
);
GO

-- MySQL creates an index on every foreign key by itself; SQL Server does not.
CREATE INDEX ix_contacts_user   ON contacts(user_id);
CREATE INDEX ix_emails_contact  ON emails(contact_id);
CREATE INDEX ix_phones_contact  ON phones(contact_id);
GO
