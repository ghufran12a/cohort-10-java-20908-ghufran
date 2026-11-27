CREATE DATABASE IF NOT EXISTS contact_db;
USE contact_db;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB; -- InnoDB is the MySQL engine that enforces foreign keys (and ON DELETE CASCADE)

CREATE TABLE contacts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    title VARCHAR(100) NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE emails (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contact_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    label VARCHAR(30) NOT NULL,

    FOREIGN KEY (contact_id)
        REFERENCES contacts(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE phones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contact_id BIGINT NOT NULL,
    phone VARCHAR(20) NOT NULL,
    label VARCHAR(30) NOT NULL,

    FOREIGN KEY (contact_id)
        REFERENCES contacts(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

