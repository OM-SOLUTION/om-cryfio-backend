CREATE TABLE accounts
(
    id                      UUID         PRIMARY KEY,
    email                   VARCHAR(320) NOT NULL,
    status                  VARCHAR(32)  NOT NULL DEFAULT 'PENDING_REGISTRATION',
    quota_bytes             BIGINT       NOT NULL DEFAULT 5368709120,

    email_verified_at       TIMESTAMPTZ,
    activated_at            TIMESTAMPTZ,
    deletion_requested_at   TIMESTAMPTZ,
    purge_scheduled_at      TIMESTAMPTZ,

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT ck_accounts_email_not_blank
        CHECK (LENGTH(email) > 0),

    CONSTRAINT ck_accounts_email_normalized
        CHECK (email = LOWER(BTRIM(email))),

    CONSTRAINT ck_accounts_status
        CHECK (
            status IN (
                       'PENDING_REGISTRATION',
                       'ACTIVE',
                       'SUSPENDED',
                       'DELETION_PENDING'
                )
            ),

    CONSTRAINT ck_accounts_quota_positive
        CHECK (quota_bytes > 0),

    CONSTRAINT ck_accounts_lock_version
        CHECK (lock_version >= 0),

    CONSTRAINT ck_accounts_deletion_schedule
        CHECK (
            purge_scheduled_at IS NULL
                OR deletion_requested_at IS NOT NULL
            )
);

-- The backend normalizes email before insertion.
-- This index also prevents duplicates if uppercase input reaches PostgreSQL.
CREATE UNIQUE INDEX uq_accounts_email_case_insensitive
    ON accounts (LOWER(email));

CREATE INDEX idx_accounts_status
    ON accounts (status);

CREATE INDEX idx_accounts_pending_registration
    ON accounts (created_at)
    WHERE status = 'PENDING_REGISTRATION';

CREATE INDEX idx_accounts_scheduled_purge
    ON accounts (purge_scheduled_at)
    WHERE purge_scheduled_at IS NOT NULL;


-- Contains only public information and an encrypted private-key backup.
-- Passwords and plaintext private keys must never be stored here.
CREATE TABLE account_encryption_material
(
    account_id              UUID         PRIMARY KEY,

    schema_version          SMALLINT     NOT NULL,

    identity_algorithm      VARCHAR(32)  NOT NULL,
    identity_public_key     BYTEA        NOT NULL,

    backup_version          SMALLINT     NOT NULL,
    backup_algorithm        VARCHAR(128) NOT NULL,
    backup_public_key       BYTEA        NOT NULL,

    kdf_algorithm           VARCHAR(32)  NOT NULL,
    kdf_salt                BYTEA        NOT NULL,
    kdf_memory_kib          INTEGER      NOT NULL,
    kdf_iterations          INTEGER      NOT NULL,
    kdf_parallelism         INTEGER      NOT NULL,
    kdf_output_bytes        INTEGER      NOT NULL,

    cipher_algorithm        VARCHAR(32)  NOT NULL,
    cipher_nonce            BYTEA        NOT NULL,
    cipher_ciphertext       BYTEA        NOT NULL,
    cipher_tag              BYTEA        NOT NULL,

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_encryption_material_account
        FOREIGN KEY (account_id)
            REFERENCES accounts (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_encryption_schema_version
        CHECK (schema_version = 1),

    CONSTRAINT ck_encryption_identity_algorithm
        CHECK (identity_algorithm = 'X25519'),

    CONSTRAINT ck_encryption_identity_public_key_size
        CHECK (OCTET_LENGTH(identity_public_key) = 32),

    CONSTRAINT ck_encryption_backup_version
        CHECK (backup_version = 1),

    CONSTRAINT ck_encryption_backup_algorithm
        CHECK (
            backup_algorithm =
            'X25519+Argon2id+HKDF-SHA256+AES-256-GCM'
            ),

    CONSTRAINT ck_encryption_backup_public_key_size
        CHECK (OCTET_LENGTH(backup_public_key) = 32),

    CONSTRAINT ck_encryption_public_keys_match
        CHECK (identity_public_key = backup_public_key),

    CONSTRAINT ck_encryption_kdf_algorithm
        CHECK (kdf_algorithm = 'Argon2id'),

    CONSTRAINT ck_encryption_kdf_salt_size
        CHECK (OCTET_LENGTH(kdf_salt) >= 16),

    CONSTRAINT ck_encryption_kdf_parameters
        CHECK (
            kdf_memory_kib = 65536
                AND kdf_iterations = 3
                AND kdf_parallelism = 4
                AND kdf_output_bytes = 32
            ),

    CONSTRAINT ck_encryption_cipher_algorithm
        CHECK (cipher_algorithm = 'AES-256-GCM'),

    CONSTRAINT ck_encryption_cipher_nonce_size
        CHECK (OCTET_LENGTH(cipher_nonce) = 12),

    CONSTRAINT ck_encryption_ciphertext_not_empty
        CHECK (OCTET_LENGTH(cipher_ciphertext) > 0),

    CONSTRAINT ck_encryption_cipher_tag_size
        CHECK (OCTET_LENGTH(cipher_tag) = 16)
);