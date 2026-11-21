CREATE TABLE account_srp_credentials (
    account_id                  UUID         PRIMARY KEY,

    srp_protocol                VARCHAR(16)  NOT NULL,
    srp_group_id                VARCHAR(32)  NOT NULL,
    srp_hash_algorithm          VARCHAR(32)  NOT NULL,
    srp_verifier                BYTEA        NOT NULL,

    password_kdf_algorithm      VARCHAR(32)  NOT NULL,
    password_kdf_salt           BYTEA        NOT NULL,
    password_kdf_memory_kib     INTEGER      NOT NULL,
    password_kdf_iterations     INTEGER      NOT NULL,
    password_kdf_parallelism    INTEGER      NOT NULL,
    password_kdf_output_bytes   INTEGER      NOT NULL,

    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_srp_credentials_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT ck_srp_protocol CHECK (srp_protocol = 'SRP-6a'),
    CONSTRAINT ck_srp_group CHECK (srp_group_id = 'RFC5054_3072'),
    CONSTRAINT ck_srp_hash_algorithm CHECK (srp_hash_algorithm = 'SHA-256'),

    CONSTRAINT ck_srp_verifier_size CHECK (OCTET_LENGTH(srp_verifier) = 384),
    CONSTRAINT ck_srp_kdf_algorithm CHECK (password_kdf_algorithm = 'Argon2id'),
    CONSTRAINT ck_srp_kdf_salt_size CHECK (OCTET_LENGTH(password_kdf_salt) >= 16),
    CONSTRAINT ck_srp_kdf_parameters CHECK (password_kdf_memory_kib = 65536 AND password_kdf_iterations = 3 AND password_kdf_parallelism = 4 AND password_kdf_output_bytes = 32)
);