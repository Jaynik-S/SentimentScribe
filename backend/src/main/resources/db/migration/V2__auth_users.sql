ALTER TABLE users
    RENAME COLUMN password TO password_hash;

ALTER TABLE users
    ADD COLUMN e2ee_kdf TEXT NOT NULL,
    ADD COLUMN e2ee_salt BYTEA NOT NULL,
    ADD COLUMN e2ee_iterations INT NOT NULL;
