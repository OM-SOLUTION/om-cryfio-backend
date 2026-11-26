CREATE TABLE account_email_verifications
(
    account_id              UUID         PRIMARY KEY,
    code_hash               VARCHAR(100) NOT NULL,
    expires_at              TIMESTAMPTZ  NOT NULL,
    failed_attempt_count    SMALLINT     NOT NULL DEFAULT 0,
    resend_available_at     TIMESTAMPTZ  NOT NULL,
    verified_at             TIMESTAMPTZ,

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_email_verifications_account
        FOREIGN KEY (account_id)
            REFERENCES accounts (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_email_verifications_failed_attempts
        CHECK (failed_attempt_count BETWEEN 0 AND 5)
);

CREATE INDEX idx_email_verifications_expires_at
    ON account_email_verifications (expires_at);