package com.sentimentscribe.usecase.delete_entry;

public class DeleteEntryOutputData {
    private final boolean success;
    private final String entryPath;

    public DeleteEntryOutputData(boolean success, String entryPath) {
        this.success = success;
        this.entryPath = entryPath;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getEntryPath() {
        return entryPath;
    }
}

