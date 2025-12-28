package com.sentimentscribe.usecase.get_recommendations;

import com.sentimentscribe.domain.MovieRecommendation;
import com.sentimentscribe.domain.SongRecommendation;
import java.util.List;

public interface GetRecommendationsUserDataAccessInterface {
    List<String> fetchKeywords(String textBody);

    List<SongRecommendation> fetchSongRecommendations(List<String> keywords,
                                                      List<String> excludeSongIds) throws Exception;

    List<MovieRecommendation> fetchMovieRecommendations(List<String> keywords,
                                                        List<String> excludeMovieIds) throws Exception;

}

