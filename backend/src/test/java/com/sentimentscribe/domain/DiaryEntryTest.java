package com.sentimentscribe.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DiaryEntryTest {

    @Test
    public void testDefaultConstructorInitializesFields() {
        DiaryEntry entry = new DiaryEntry();
        assertEquals("", entry.getTitleCiphertext());
        assertEquals("", entry.getBodyCiphertext());
        assertEquals("AES-GCM", entry.getAlgo());
        assertEquals(1, entry.getVersion());
        assertNotNull(entry.getCreatedAt());
        assertEquals(entry.getCreatedAt(), entry.getUpdatedAt());
    }

    @Test
    public void testParameterizedConstructorInitializesFields() {
        LocalDateTime created = LocalDateTime.of(2024, 1, 1, 12, 0);
        DiaryEntry entry = new DiaryEntry(
                "title",
                "title-iv",
                "body",
                "body-iv",
                "AES-GCM",
                2,
                "entries/1.json",
                created,
                created
        );

        assertEquals("title", entry.getTitleCiphertext());
        assertEquals("body", entry.getBodyCiphertext());
        assertEquals("AES-GCM", entry.getAlgo());
        assertEquals(2, entry.getVersion());
        assertEquals(created, entry.getCreatedAt());
        assertEquals(created, entry.getUpdatedAt());
    }

    @Test
    public void testSettersAndGetters() {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitleCiphertext("Title");
        entry.setTitleIv("title-iv");
        entry.setBodyCiphertext("Body");
        entry.setBodyIv("body-iv");
        entry.setAlgo("AES-GCM");
        entry.setVersion(3);

        assertEquals("Title", entry.getTitleCiphertext());
        assertEquals("title-iv", entry.getTitleIv());
        assertEquals("Body", entry.getBodyCiphertext());
        assertEquals("body-iv", entry.getBodyIv());
        assertEquals("AES-GCM", entry.getAlgo());
        assertEquals(3, entry.getVersion());
    }

    @Test
    public void testUpdatedTimeChangesUpdatedAt() throws InterruptedException {
        DiaryEntry entry = new DiaryEntry();
        LocalDateTime firstUpdated = entry.getUpdatedAt();
        Thread.sleep(5); // ensure time changes at millisecond resolution
        entry.updatedTime();
        LocalDateTime secondUpdated = entry.getUpdatedAt();
        assertFalse(secondUpdated.isBefore(firstUpdated));
    }

    @Test
    public void testStoragePathCanBeSet() {
        DiaryEntry entry = new DiaryEntry();
        entry.setStoragePath("entries/1.json");
        assertEquals("entries/1.json", entry.getStoragePath());
    }

    @Test
    public void testRecommendationsListsCanBeSet() {
        DiaryEntry entry = new DiaryEntry();
        SongRecommendation song = new SongRecommendation("track-1", "2020", "img", "song", "artist", "90", "url");
        MovieRecommendation movie = new MovieRecommendation("movie-1", "2019", "img2", "title", "8", "overview");

        entry.setSongRecommendations(List.of(song));
        entry.setMovieRecommendations(List.of(movie));

        assertEquals(1, entry.getSongRecommendations().size());
        assertEquals(song, entry.getSongRecommendations().get(0));
        assertEquals(1, entry.getMovieRecommendations().size());
        assertEquals(movie, entry.getMovieRecommendations().get(0));
    }
}

