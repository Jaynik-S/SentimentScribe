package com.moodverse.usecase.get_recommendations;

import com.moodverse.domain.MovieRecommendation;
import com.moodverse.domain.SongRecommendation;
import java.util.List;

public interface GetRecommendationsUserDataAccessInterface {
    List<String> fetchKeywords(String textBody);

    List<SongRecommendation> fetchSongRecommendations(List<String> keywords) throws Exception;

    List<MovieRecommendation> fetchMovieRecommendations(List<String> keywords) throws Exception;

}

