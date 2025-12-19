package com.moodverse.adapter.recommendation_menu;

import com.moodverse.adapter.ViewManagerModel;
import com.moodverse.usecase.get_recommendations.GetRecommendationsOutputBoundary;
import com.moodverse.usecase.get_recommendations.GetRecommendationsOutputData;

public class RecommendationMenuPresenter implements GetRecommendationsOutputBoundary {

    private final RecommendationMenuViewModel recommendationMenuViewModel;
    private final ViewManagerModel viewManagerModel;

    public RecommendationMenuPresenter(RecommendationMenuViewModel recommendationMenuViewModel,
                                       ViewManagerModel viewManagerModel) {
        this.recommendationMenuViewModel = recommendationMenuViewModel;
        this.viewManagerModel = viewManagerModel;
    }

    @Override
    public void prepareSuccessView(GetRecommendationsOutputData outputData) {
        final RecommendationMenuState recommendationMenuState = recommendationMenuViewModel.getState();
        recommendationMenuState.setSongRecommendation(outputData.getSongRecommendations());
        recommendationMenuState.setMovieRecommendation(outputData.getMovieRecommendations());
        recommendationMenuViewModel.firePropertyChanged();
        recommendationMenuViewModel.setSongRecommendation(outputData.getSongRecommendations());
        recommendationMenuViewModel.setMovieRecommendation(outputData.getMovieRecommendations());
    }

    @Override
    public void prepareFailView(String error) {
        final RecommendationMenuState recommendationMenuState = recommendationMenuViewModel.getState();
        recommendationMenuState.setError(error);
        recommendationMenuViewModel.setError(error);
        recommendationMenuViewModel.firePropertyChanged();
    }

    @Override
    public void switchToRecommendationMenu() {
        viewManagerModel.setState(recommendationMenuViewModel.getViewName());
        viewManagerModel.firePropertyChanged();
    }
}


