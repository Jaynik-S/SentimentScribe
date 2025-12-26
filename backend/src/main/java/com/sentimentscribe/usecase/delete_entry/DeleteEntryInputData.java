package com.sentimentscribe.usecase.delete_entry;

import java.util.UUID;

public class DeleteEntryInputData {

    private final UUID userId;
    private final String entryPath;

    public DeleteEntryInputData(UUID userId, String entryPath) {

        this.userId = userId;
        this.entryPath = entryPath;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEntryPath() {
        return entryPath;
    }
}


