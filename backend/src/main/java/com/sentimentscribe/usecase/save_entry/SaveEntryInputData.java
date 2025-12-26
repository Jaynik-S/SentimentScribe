package com.sentimentscribe.usecase.save_entry;

import java.time.LocalDateTime;
import java.util.UUID;

public class SaveEntryInputData {
    private final UUID userId;
    private final String titleCiphertext;
    private final String titleIv;
    private final String bodyCiphertext;
    private final String bodyIv;
    private final String algo;
    private final int version;
    private final String storagePath;
    private final LocalDateTime createdAt;

    public SaveEntryInputData(UUID userId,
                              String titleCiphertext,
                              String titleIv,
                              String bodyCiphertext,
                              String bodyIv,
                              String algo,
                              int version,
                              String storagePath,
                              LocalDateTime createdAt) {
        this.userId = userId;
        this.titleCiphertext = titleCiphertext;
        this.titleIv = titleIv;
        this.bodyCiphertext = bodyCiphertext;
        this.bodyIv = bodyIv;
        this.algo = algo;
        this.version = version;
        this.storagePath = storagePath;
        this.createdAt = createdAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitleCiphertext() {
        return titleCiphertext;
    }

    public String getTitleIv() {
        return titleIv;
    }

    public String getBodyCiphertext() {
        return bodyCiphertext;
    }

    public String getBodyIv() {
        return bodyIv;
    }

    public String getAlgo() {
        return algo;
    }

    public int getVersion() {
        return version;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}

