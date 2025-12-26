package com.sentimentscribe.domain;

import java.time.LocalDateTime;
import java.util.List;

public class DiaryEntry {
    public static final int MAX_TITLE_LENGTH = 30;
    public static final int MIN_TEXT_LENGTH = 50;
    public static final int MAX_TEXT_LENGTH = 1000;

    private String titleCiphertext;
    private String titleIv;
    private String bodyCiphertext;
    private String bodyIv;
    private String algo;
    private int version;
    /**
     * Storage identifier owned by the persistence layer (e.g., a file path).
     * This is kept on the entity so use cases can preserve identity across edits.
     */
    private String storagePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SongRecommendation> songRecommendations;
    private List<MovieRecommendation> movieRecommendations;

    public DiaryEntry() {
        this.titleCiphertext = "";
        this.titleIv = "";
        this.bodyCiphertext = "";
        this.bodyIv = "";
        this.algo = "AES-GCM";
        this.version = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    public DiaryEntry(String titleCiphertext,
                      String titleIv,
                      String bodyCiphertext,
                      String bodyIv,
                      String algo,
                      int version,
                      String storagePath,
                      LocalDateTime createdAt,
                      LocalDateTime updatedAt) {
        this.titleCiphertext = titleCiphertext;
        this.titleIv = titleIv;
        this.bodyCiphertext = bodyCiphertext;
        this.bodyIv = bodyIv;
        this.algo = algo;
        this.version = version;
        this.storagePath = storagePath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public List<SongRecommendation> getSongRecommendations() {
        return songRecommendations;
    }

    public List<MovieRecommendation> getMovieRecommendations() {
        return movieRecommendations;
    }

    public void setTitleCiphertext(String titleCiphertext) {
        this.titleCiphertext = titleCiphertext;
    }

    public void setTitleIv(String titleIv) {
        this.titleIv = titleIv;
    }

    public void setBodyCiphertext(String bodyCiphertext) {
        this.bodyCiphertext = bodyCiphertext;
    }

    public void setBodyIv(String bodyIv) {
        this.bodyIv = bodyIv;
    }

    public void setAlgo(String algo) {
        this.algo = algo;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public void setSongRecommendations(List<SongRecommendation> songRecommendations) {
        this.songRecommendations = songRecommendations;
    }

    public void setMovieRecommendations(List<MovieRecommendation> movieRecommendations) {
        this.movieRecommendations = movieRecommendations;
    }

    public void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updatedTime() {
        touchUpdatedAt();
    }

}

