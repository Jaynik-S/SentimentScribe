package com.sentimentscribe.usecase.get_recommendations;

import java.util.List;

public class GetRecommendationsInputData {
    private final String textBody;
    private final List<String> excludeSongIds;
    private final List<String> excludeMovieIds;

    public GetRecommendationsInputData(String textBody,
                                       List<String> excludeSongIds,
                                       List<String> excludeMovieIds) {
        this.textBody = textBody;
        this.excludeSongIds = excludeSongIds;
        this.excludeMovieIds = excludeMovieIds;
    }

    public String getTextBody() {
        return textBody;
    }

    public List<String> getExcludeSongIds() {
        return excludeSongIds;
    }

    public List<String> getExcludeMovieIds() {
        return excludeMovieIds;
    }
}


