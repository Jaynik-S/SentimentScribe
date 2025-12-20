package com.moodverse.data;

import com.moodverse.config.SpotifyProperties;
import com.moodverse.config.TmdbProperties;
import com.moodverse.domain.MovieRecommendation;
import com.moodverse.domain.SongRecommendation;
import com.moodverse.domain.Keyword;
import com.moodverse.usecase.get_recommendations.GetRecommendationsUserDataAccessInterface;

import java.util.List;

public class RecommendationAPIAccessObject implements GetRecommendationsUserDataAccessInterface {

    private final NLPAnalysisDataAccessObject nlpAnalysisDataAccessObject;
    private final SpotifyProperties spotifyProperties;
    private final TmdbProperties tmdbProperties;

    public RecommendationAPIAccessObject() {
        this(NLPAnalysisDataAccessObject.createWithDefaultPipeline(), new SpotifyProperties(null, null),
                new TmdbProperties(null));
    }

    public RecommendationAPIAccessObject(NLPAnalysisDataAccessObject nlpAnalysisDataAccessObject,
                                         SpotifyProperties spotifyProperties,
                                         TmdbProperties tmdbProperties) {
        this.nlpAnalysisDataAccessObject = nlpAnalysisDataAccessObject;
        this.spotifyProperties = spotifyProperties;
        this.tmdbProperties = tmdbProperties;
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
            SpotifyAPIAccessObject spotifyAPI = new SpotifyAPIAccessObject(keywords, spotifyProperties);
            return spotifyAPI.fetchSongRecommendations();
        }
        catch (Exception error) {
            throw new Exception("Error fetching song recommendations: " + error.getMessage());
        }
    }

    @Override
    public List<MovieRecommendation> fetchMovieRecommendations(List<String> keywords) throws Exception {
        try {
            TMDbAPIAccessObject tmdbAPI = new TMDbAPIAccessObject(keywords, tmdbProperties);
            return tmdbAPI.fetchMovieRecommendations();
        }
        catch (Exception error) {
            throw new Exception("Error fetching movie recommendations: " + error.getMessage());
        }
    }

}

