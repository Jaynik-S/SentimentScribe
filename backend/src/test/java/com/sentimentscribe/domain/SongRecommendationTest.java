package com.sentimentscribe.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SongRecommendationTest {

    @Test
    public void testSongRecommendationStoresAllFields() {
        SongRecommendation rec =
                new SongRecommendation("track-1", "2020", "img", "song", "artist", "90/100", "url");
        assertEquals("track-1", rec.getSongId());
        assertEquals("2020", rec.getReleaseYear());
        assertEquals("img", rec.getImageUrl());
        assertEquals("song", rec.getSongName());
        assertEquals("artist", rec.getArtistName());
        assertEquals("90/100", rec.getPopularityScore());
        assertEquals("url", rec.getExternalUrl());
    }

    @Test
    public void testSongRecommendationAllowsNullImageAndUrl() {
        SongRecommendation rec = new SongRecommendation("track-2", "1999", null, "name", "artist", "50", null);
        assertEquals("track-2", rec.getSongId());
        assertEquals("1999", rec.getReleaseYear());
        assertNull(rec.getImageUrl());
        assertNull(rec.getExternalUrl());
    }
}


