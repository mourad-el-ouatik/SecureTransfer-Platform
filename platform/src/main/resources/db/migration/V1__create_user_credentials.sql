CREATE TABLE IF NOT EXISTS user_credentials (
                                                id UUID PRIMARY KEY,
                                                email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    mfa_secret VARCHAR(255),
    mfa_enabled BOOLEAN DEFAULT FALSE,
    failed_attempts INTEGER DEFAULT 0,
    account_locked BOOLEAN DEFAULT FALSE,
    lock_time TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
    );