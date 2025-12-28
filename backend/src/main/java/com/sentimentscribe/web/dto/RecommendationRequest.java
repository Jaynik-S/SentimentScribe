package com.sentimentscribe.web.dto;

import java.util.List;

public record RecommendationRequest(
        String text,
        List<String> excludeSongIds,
        List<String> excludeMovieIds
) {
}
