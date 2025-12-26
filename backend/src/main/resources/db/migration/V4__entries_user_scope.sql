ALTER TABLE diary_entries
    DROP CONSTRAINT IF EXISTS diary_entries_storage_path_key;

ALTER TABLE diary_entries
    ADD CONSTRAINT diary_entries_user_storage_path_key UNIQUE (user_id, storage_path);

CREATE INDEX IF NOT EXISTS diary_entries_user_updated_at_idx
    ON diary_entries (user_id, updated_at DESC);
