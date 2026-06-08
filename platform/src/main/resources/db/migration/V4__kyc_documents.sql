CREATE TABLE kyc_documents (
                               id              BIGSERIAL PRIMARY KEY,
                               user_id         BIGINT NOT NULL,
                               user_type       VARCHAR(20) NOT NULL, -- 'PARTICULIER', 'AGENCE', 'ENTREPRISE'
                               document_type   VARCHAR(50) NOT NULL, -- 'CIN', 'PASSPORT', 'LICENSE', etc.
                               file_name       VARCHAR(255) NOT NULL,
                               file_path       TEXT NOT NULL,
                               file_hash       VARCHAR(64) NOT NULL,  -- SHA-256 hex
                               mime_type       VARCHAR(100) NOT NULL,
                               file_size       BIGINT NOT NULL,
                               kyc_state       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               rejection_reason TEXT,
                               submitted_at    TIMESTAMP,
                               reviewed_at     TIMESTAMP,
                               reviewed_by     VARCHAR(255),
                               created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                               updated_at      TIMESTAMP
);

CREATE INDEX idx_kyc_documents_user ON kyc_documents(user_id, user_type);
CREATE INDEX idx_kyc_documents_state ON kyc_documents(kyc_state);