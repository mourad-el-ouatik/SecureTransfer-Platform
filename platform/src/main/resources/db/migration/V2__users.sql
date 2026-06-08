-- V2__users.sql
-- Tables RBAC (Rôles et Permissions)

CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE permissions (
                             id BIGSERIAL PRIMARY KEY,
                             name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE role_permissions (
                                  role_id BIGINT REFERENCES roles(id),
                                  permission_id BIGINT REFERENCES permissions(id),
                                  PRIMARY KEY (role_id, permission_id)
);

-- Table Particuliers
CREATE TABLE particuliers (
                              id BIGSERIAL PRIMARY KEY,
                              email VARCHAR(255) UNIQUE NOT NULL,
                              phone_number TEXT,          -- Stocké chiffré AES-256
                              cin TEXT NOT NULL,          -- CIN chiffré AES-256
                              kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                              version BIGINT NOT NULL DEFAULT 0,
                              daily_transaction_limit NUMERIC(15,2) NOT NULL DEFAULT 10000.00,
                              single_transaction_limit NUMERIC(15,2) NOT NULL DEFAULT 2000.00,
                              date_of_birth DATE,
                              nationality VARCHAR(50),
                              created_at TIMESTAMP NOT NULL,
                              updated_at TIMESTAMP
);

-- Table Agences
CREATE TABLE agences (
                         id BIGSERIAL PRIMARY KEY,
                         email VARCHAR(255) UNIQUE NOT NULL,
                         phone_number TEXT,
                         license_number TEXT,        -- Chiffré AES-256
                         kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                         version BIGINT NOT NULL DEFAULT 0,
                         daily_transaction_limit NUMERIC(15,2) NOT NULL DEFAULT 500000.00,
                         single_transaction_limit NUMERIC(15,2) NOT NULL DEFAULT 100000.00,
                         city VARCHAR(100),
                         address TEXT,
                         cash_balance NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                         created_at TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP
);

-- Table Entreprises
CREATE TABLE entreprises (
                             id BIGSERIAL PRIMARY KEY,
                             email VARCHAR(255) UNIQUE NOT NULL,
                             phone_number TEXT,
                             registration_number VARCHAR(50),
                             legal_name VARCHAR(200),
                             kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                             version BIGINT NOT NULL DEFAULT 0,
                             daily_transaction_limit NUMERIC(15,2) NOT NULL DEFAULT 2000000.00,
                             single_transaction_limit NUMERIC(15,2) NOT NULL DEFAULT 500000.00,
                             created_at TIMESTAMP NOT NULL,
                             updated_at TIMESTAMP
);

-- Liaison utilisateurs ↔ rôles (table polymorphique)
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT REFERENCES roles(id),
                            PRIMARY KEY (user_id, role_id)
);

-- Données initiales : rôles
INSERT INTO roles (name) VALUES
                             ('ROLE_USER'),
                             ('ROLE_AGENCE'),
                             ('ROLE_ADMIN');

-- Données initiales : permissions
INSERT INTO permissions (name) VALUES
                                   ('user:read'), ('user:write'),
                                   ('transaction:initiate'), ('transaction:view'),
                                   ('kyc:submit'), ('kyc:validate');

-- Index pour performances
CREATE INDEX idx_particuliers_kyc ON particuliers(kyc_status);
CREATE INDEX idx_particuliers_email ON particuliers(email);
CREATE INDEX idx_agences_email ON agences(email);
