CREATE TABLE account_sessions
(
    id                      UUID         PRIMARY KEY,

    account_id              UUID         NOT NULL,
    refresh_token_hash      BYTEA        NOT NULL,

    device_name             VARCHAR(128) NOT NULL,

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at              TIMESTAMPTZ  NOT NULL,
    revoked_at              TIMESTAMPTZ,

    lock_version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_account_sessions_account
        FOREIGN KEY (account_id)
            REFERENCES accounts (id)
            ON DELETE CASCADE,

    -- SHA-256 output is always 32 bytes.
    CONSTRAINT ck_account_sessions_refresh_token_hash_size
        CHECK (OCTET_LENGTH(refresh_token_hash) = 32),

    CONSTRAINT ck_account_sessions_expiry
        CHECK (expires_at > created_at),

    CONSTRAINT ck_account_sessions_revocation_time
        CHECK (
            revoked_at IS NULL
                OR revoked_at >= created_at
            ),

    CONSTRAINT ck_account_sessions_lock_version
        CHECK (lock_version >= 0)
);

CREATE UNIQUE INDEX uq_account_sessions_refresh_token_hash
    ON account_sessions (refresh_token_hash);

CREATE INDEX idx_account_sessions_account_active
    ON account_sessions (account_id, last_activity_at DESC)
    WHERE revoked_at IS NULL;