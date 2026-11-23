CREATE TABLE resources
(
    id                      UUID         PRIMARY KEY,

    resource_type           VARCHAR(16)  NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    file_extension          VARCHAR(255),

    parent_folder_id        UUID,
    owner_account_id        UUID         NOT NULL,

    created_by_account_id   UUID         NOT NULL,
    modified_by_account_id  UUID         NOT NULL,

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    lock_version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT ck_resources_type
        CHECK (resource_type IN ('FILE', 'FOLDER')),

    CONSTRAINT ck_resources_name_not_blank
        CHECK (LENGTH(TRIM(name)) > 0),

    CONSTRAINT ck_resources_folder_has_no_extension
        CHECK (
            resource_type <> 'FOLDER'
                OR file_extension IS NULL
            ),

    CONSTRAINT ck_resources_lock_version
        CHECK (lock_version >= 0),

    CONSTRAINT fk_resources_parent_folder
        FOREIGN KEY (parent_folder_id)
            REFERENCES resources (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_resources_owner
        FOREIGN KEY (owner_account_id)
            REFERENCES accounts (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_resources_creator
        FOREIGN KEY (created_by_account_id)
            REFERENCES accounts (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_resources_modifier
        FOREIGN KEY (modified_by_account_id)
            REFERENCES accounts (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_resources_parent_folder
    ON resources (parent_folder_id);

CREATE INDEX idx_resources_owner
    ON resources (owner_account_id);


CREATE TABLE file_versions
(
    id                      UUID         PRIMARY KEY,

    resource_id             UUID         NOT NULL,
    version_number          INTEGER      NOT NULL,

    status                  VARCHAR(32)  NOT NULL,

    storage_object_key      VARCHAR(255) NOT NULL,

    plaintext_size_bytes    BIGINT       NOT NULL,
    encrypted_size_bytes    BIGINT       NOT NULL,
    ciphertext_sha256       BYTEA        NOT NULL,
    chunk_count             INTEGER      NOT NULL,

    uploaded_by_account_id  UUID         NOT NULL,

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at            TIMESTAMPTZ,

    lock_version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uq_file_versions_resource_number
        UNIQUE (resource_id, version_number),

    CONSTRAINT uq_file_versions_storage_object_key
        UNIQUE (storage_object_key),

    CONSTRAINT ck_file_versions_status
        CHECK (status IN ('PENDING_UPLOAD', 'ACTIVE')),

    CONSTRAINT ck_file_versions_number
        CHECK (version_number >= 1),

    CONSTRAINT ck_file_versions_plaintext_size
        CHECK (plaintext_size_bytes >= 0),

    CONSTRAINT ck_file_versions_encrypted_size
        CHECK (encrypted_size_bytes > 0),

    CONSTRAINT ck_file_versions_ciphertext_hash_size
        CHECK (OCTET_LENGTH(ciphertext_sha256) = 32),

    CONSTRAINT ck_file_versions_chunk_count
        CHECK (chunk_count >= 1),

    CONSTRAINT ck_file_versions_activation
        CHECK (
            (status = 'PENDING_UPLOAD' AND activated_at IS NULL)
                OR
            (status = 'ACTIVE' AND activated_at IS NOT NULL)
            ),

    CONSTRAINT ck_file_versions_lock_version
        CHECK (lock_version >= 0),

    CONSTRAINT fk_file_versions_resource
        FOREIGN KEY (resource_id)
            REFERENCES resources (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_file_versions_uploader
        FOREIGN KEY (uploaded_by_account_id)
            REFERENCES accounts (id)
            ON DELETE RESTRICT
);

-- A file has exactly one visible/current version.
CREATE UNIQUE INDEX uq_file_versions_active_resource
    ON file_versions (resource_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_file_versions_resource
    ON file_versions (resource_id, version_number DESC);


CREATE TABLE file_version_key_envelopes
(
    file_version_id         UUID         NOT NULL,
    recipient_account_id    UUID         NOT NULL,

    encrypted_dek           BYTEA        NOT NULL,

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_file_version_key_envelopes
        PRIMARY KEY (file_version_id, recipient_account_id),

    CONSTRAINT ck_file_version_key_envelopes_not_empty
        CHECK (OCTET_LENGTH(encrypted_dek) > 0),

    CONSTRAINT fk_file_version_key_envelopes_version
        FOREIGN KEY (file_version_id)
            REFERENCES file_versions (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_file_version_key_envelopes_recipient
        FOREIGN KEY (recipient_account_id)
            REFERENCES accounts (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_file_version_key_envelopes_recipient
    ON file_version_key_envelopes (recipient_account_id);