CREATE TABLE transactions (
    id                BIGSERIAL PRIMARY KEY,
    sender_id         BIGINT NOT NULL,
    receiver_id       BIGINT NOT NULL,
    amount            NUMERIC(19, 2) NOT NULL,
    fee               NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    type              VARCHAR(20) NOT NULL,
    idempotency_key   VARCHAR(255) UNIQUE NOT NULL,
    withdrawal_code   VARCHAR(100),
    withdrawal_expires_at TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP
);

CREATE TABLE fraud_alerts (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT NOT NULL REFERENCES transactions(id),
    alert_type      VARCHAR(50) NOT NULL,
    description     TEXT,
    severity        VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
