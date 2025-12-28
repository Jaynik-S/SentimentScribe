package com.sentimentscribe.web.dto;

public record MovieRecommendationResponse(
        String movieId,
        String releaseYear,
        String imageUrl,
        String movieTitle,
        String movieRating,
        String overview
) {
}
