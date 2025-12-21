package com.sentimentscribe.domain;

import java.time.LocalDateTime;
import java.util.List;

public class DiaryEntry {
    public static final int MAX_TITLE_LENGTH = 30;
    public static final int MIN_TEXT_LENGTH = 50;
    public static final int MAX_TEXT_LENGTH = 1000;

    private String title;
    private String text;
    private List<String> keywords;
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
        this.title = "Untitled Document";
        this.text = "Enter your text here...";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    public DiaryEntry(String title, String textBody, LocalDateTime date) {
        this.title = title;
        this.text = textBody;
        this.createdAt = date;
        this.updatedAt = date;
    }

    public DiaryEntry(String title,
                      String textBody,
                      List<String> keywords,
                      String storagePath,
                      LocalDateTime createdAt,
                      LocalDateTime updatedAt) {
        this.title = title;
        this.text = textBody;
        this.keywords = keywords;
        this.storagePath = storagePath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public List<String> getKeywords() {
        return keywords;
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

    public void setTitle(String title) {
        this.title = title;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setKeywords(List<String> keyword) {
        this.keywords = keyword;
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

