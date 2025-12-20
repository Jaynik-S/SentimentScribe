package com.moodverse.usecase.delete_entry;

public class DeleteEntryInputData {

    private final String entryPath;

    public DeleteEntryInputData(String entryPath) {

        this.entryPath = entryPath;
    }

    public String getEntryPath() {
        return entryPath;
    }
}


