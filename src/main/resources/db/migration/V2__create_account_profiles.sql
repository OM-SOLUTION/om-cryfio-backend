CREATE TABLE account_profiles
(
    account_id     UUID         PRIMARY KEY,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    username       VARCHAR(30),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_profiles_account
        FOREIGN KEY (account_id)
            REFERENCES accounts (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_account_profiles_first_name
        CHECK (
            first_name = BTRIM(first_name)
                AND CHAR_LENGTH(first_name) BETWEEN 1 AND 100
            ),

    CONSTRAINT ck_account_profiles_last_name
        CHECK (
            last_name = BTRIM(last_name)
                AND CHAR_LENGTH(last_name) BETWEEN 1 AND 100
            ),

    CONSTRAINT ck_account_profiles_username
        CHECK (
            username IS NULL
                OR (
                username = LOWER(BTRIM(username))
                    AND username ~ '^[a-z0-9_]{3,30}$'
                )
            )
);

-- Multiple NULL usernames are allowed, but non-null usernames must be unique.
CREATE UNIQUE INDEX uq_account_profiles_username
    ON account_profiles (LOWER(username))
    WHERE username IS NOT NULL;