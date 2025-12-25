package com.sentimentscribe.usecase.load_entry;

import java.util.UUID;

public class LoadEntryInputData {

    private final UUID userId;
    private final String entryPath;

    public LoadEntryInputData(UUID userId, String entryPath) {
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


