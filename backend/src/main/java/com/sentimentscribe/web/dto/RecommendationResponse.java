package com.sentimentscribe.web.dto;

import java.util.List;

public record RecommendationResponse(
        List<String> keywords,
        List<SongRecommendationResponse> songs,
        List<MovieRecommendationResponse> movies
) {
}
