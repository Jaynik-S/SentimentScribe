package com.moodverse.web.dto;

public record SongRecommendationResponse(
        String releaseYear,
        String imageUrl,
        String songName,
        String artistName,
        String popularityScore,
        String externalUrl
) {
}
