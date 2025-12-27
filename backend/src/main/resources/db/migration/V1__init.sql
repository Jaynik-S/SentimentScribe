CREATE TABLE users (
    id UUID PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    e2ee_kdf TEXT NOT NULL,
    e2ee_salt BYTEA NOT NULL,
    e2ee_iterations INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE diary_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    storage_path TEXT NOT NULL,
    title_ciphertext TEXT NOT NULL DEFAULT '',
    title_iv TEXT NOT NULL DEFAULT '',
    body_ciphertext TEXT NOT NULL DEFAULT '',
    body_iv TEXT NOT NULL DEFAULT '',
    algo TEXT NOT NULL DEFAULT 'AES-GCM',
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT diary_entries_user_storage_path_key UNIQUE (user_id, storage_path)
);

CREATE INDEX IF NOT EXISTS diary_entries_user_updated_at_idx
    ON diary_entries (user_id, updated_at DESC);
