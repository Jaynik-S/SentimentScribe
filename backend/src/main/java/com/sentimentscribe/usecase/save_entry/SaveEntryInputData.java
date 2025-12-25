package com.sentimentscribe.usecase.save_entry;

import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

public class SaveEntryInputData {
    private final UUID userId;
    private final String title;
    private final LocalDateTime date;
    private final String textBody;
    private final String storagePath;
    private final List<String> keywords;

    public SaveEntryInputData(UUID userId,
                              String title,
                              LocalDateTime date,
                              String textBody,
                              String storagePath,
                              List<String> keywords) {
        this.userId = userId;
        this.title = title;
        this.date = date;
        this.textBody = textBody;
        this.storagePath = storagePath;
        this.keywords = keywords;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getTextBody() {
        return textBody;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public List<String> getKeywords() {
        return keywords;
    }

}

