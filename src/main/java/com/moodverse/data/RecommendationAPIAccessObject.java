package com.moodverse.data;

import com.moodverse.domain.MovieRecommendation;
import com.moodverse.domain.SongRecommendation;
import com.moodverse.domain.Keyword;
import com.moodverse.usecase.get_recommendations.GetRecommendationsUserDataAccessInterface;

import java.util.List;

public class RecommendationAPIAccessObject implements GetRecommendationsUserDataAccessInterface {

    private final NLPAnalysisDataAccessObject nlpAnalysisDataAccessObject;

    public RecommendationAPIAccessObject() {
        this(NLPAnalysisDataAccessObject.createWithDefaultPipeline());
    }

    public RecommendationAPIAccessObject(NLPAnalysisDataAccessObject nlpAnalysisDataAccessObject) {
        this.nlpAnalysisDataAccessObject = nlpAnalysisDataAccessObject;
    }

    @Override
    public List<String> fetchKeywords(String textBody) {
        return nlpAnalysisDataAccessObject.analyze(textBody)
                .keywords()
                .stream()
                .map(Keyword::text)
                .toList();
    }

    @Override
    public List<SongRecommendation> fetchSongRecommendations(List<String> keywords) throws Exception {
        try {
            SpotifyAPIAccessObject spotifyAPI = new SpotifyAPIAccessObject(keywords);
            return spotifyAPI.fetchSongRecommendations();
        }
        catch (Exception error) {
            throw new Exception("Error fetching song recommendations: " + error.getMessage());
        }
    }

    @Override
    public List<MovieRecommendation> fetchMovieRecommendations(List<String> keywords) throws Exception {
        try {
            TMDbAPIAccessObject tmdbAPI = new TMDbAPIAccessObject(keywords);
            return tmdbAPI.fetchMovieRecommendations();
        }
        catch (Exception error) {
            throw new Exception("Error fetching movie recommendations: " + error.getMessage());
        }
    }

}

