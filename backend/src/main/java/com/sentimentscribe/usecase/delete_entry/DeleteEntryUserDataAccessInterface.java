package com.sentimentscribe.usecase.delete_entry;

import java.util.UUID;

public interface DeleteEntryUserDataAccessInterface {
    boolean deleteByPath(UUID userId, String entryPath);
}

