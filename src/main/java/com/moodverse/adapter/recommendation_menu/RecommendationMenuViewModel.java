package com.moodverse.adapter.recommendation_menu;

import com.moodverse.domain.MovieRecommendation;
import com.moodverse.domain.SongRecommendation;
import com.moodverse.adapter.ViewModel;

import java.util.List;

public class RecommendationMenuViewModel extends ViewModel<RecommendationMenuState> {

    private List<SongRecommendation> songRecommendations;
    private List<MovieRecommendation> movieRecommendations;

    public void setSongRecommendation(List<SongRecommendation> songRecommendation) {
        this.songRecommendations = songRecommendation;
    }

    public void setMovieRecommendation(List<MovieRecommendation> MovieRecommendations) {
        this.movieRecommendations = MovieRecommendations;
    }

    public List<SongRecommendation> getSongRecommendations() {
        return songRecommendations;
    }

    public List<MovieRecommendation> getMovieRecommendations() {
        return movieRecommendations;
    }

    private String displayError;

    public void setError(String displayError) {

        this.displayError = displayError;
    }

    public String getError() {
        return displayError;
    }

    public RecommendationMenuViewModel() {
        super("recommendation_menu");
        setState(new RecommendationMenuState());
    }
}


