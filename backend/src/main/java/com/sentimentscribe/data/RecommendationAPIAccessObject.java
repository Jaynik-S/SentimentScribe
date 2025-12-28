package com.sentimentscribe.data;

import com.sentimentscribe.config.SpotifyProperties;
import com.sentimentscribe.config.TmdbProperties;
import com.sentimentscribe.domain.MovieRecommendation;
import com.sentimentscribe.domain.SongRecommendation;
import com.sentimentscribe.domain.Keyword;
import com.sentimentscribe.usecase.get_recommendations.GetRecommendationsUserDataAccessInterface;

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
    public List<SongRecommendation> fetchSongRecommendations(List<String> keywords,
                                                             List<String> excludeSongIds) throws Exception {
        try {
            SpotifyAPIAccessObject spotifyAPI = new SpotifyAPIAccessObject(keywords, spotifyProperties);
            return spotifyAPI.fetchSongRecommendations(excludeSongIds);
        }
        catch (Exception error) {
            throw new Exception("Error fetching song recommendations: " + error.getMessage());
        }
    }

    @Override
    public List<MovieRecommendation> fetchMovieRecommendations(List<String> keywords,
                                                               List<String> excludeMovieIds) throws Exception {
        try {
            TMDbAPIAccessObject tmdbAPI = new TMDbAPIAccessObject(keywords, tmdbProperties);
            return tmdbAPI.fetchMovieRecommendations(excludeMovieIds);
        }
        catch (Exception error) {
            throw new Exception("Error fetching movie recommendations: " + error.getMessage());
        }
    }

}

