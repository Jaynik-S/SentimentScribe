package com.sentimentscribe.usecase.save_entry;

import java.util.List;
import java.time.LocalDateTime;

public class SaveEntryOutputData {
    private final String title;
    private final String text;
    private final LocalDateTime date;
    private final String storagePath;
    private final List<String> keywords;
    private final boolean success;

    public SaveEntryOutputData(String title,
                               String text,
                               LocalDateTime date,
                               String storagePath,
                               List<String> keywords,
                               boolean success) {
        this.title = title;
        this.text = text;
        this.date = date;
        this.storagePath = storagePath;
        this.keywords = keywords;
        this.success = success;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public boolean isSuccess() {
        return success;
    }

}

