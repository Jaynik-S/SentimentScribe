package com.sentimentscribe.usecase.save_entry;

import java.time.LocalDateTime;

public class SaveEntryOutputData {
    private final String titleCiphertext;
    private final String titleIv;
    private final String bodyCiphertext;
    private final String bodyIv;
    private final String algo;
    private final int version;
    private final String storagePath;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final boolean success;

    public SaveEntryOutputData(String titleCiphertext,
                               String titleIv,
                               String bodyCiphertext,
                               String bodyIv,
                               String algo,
                               int version,
                               String storagePath,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt,
                               boolean success) {
        this.titleCiphertext = titleCiphertext;
        this.titleIv = titleIv;
        this.bodyCiphertext = bodyCiphertext;
        this.bodyIv = bodyIv;
        this.algo = algo;
        this.version = version;
        this.storagePath = storagePath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.success = success;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isSuccess() {
        return success;
    }

}

