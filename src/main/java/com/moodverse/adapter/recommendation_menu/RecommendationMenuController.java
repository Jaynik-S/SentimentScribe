package com.moodverse.adapter.recommendation_menu;

import com.moodverse.usecase.go_back.GoBackInputBoundary;
import com.moodverse.usecase.get_recommendations.GetRecommendationsInputBoundary;
import com.moodverse.usecase.get_recommendations.GetRecommendationsInputData;

public class RecommendationMenuController {
    private final GetRecommendationsInputBoundary getRecommendationInteractor;

    private final GoBackInputBoundary gobackInteractor;

    public RecommendationMenuController(GetRecommendationsInputBoundary getRecommendationInteractor,
                                        GoBackInputBoundary backInteractor) {
        this.getRecommendationInteractor = getRecommendationInteractor;
        this.gobackInteractor = backInteractor;
    }

    public void execute(GetRecommendationsInputData inputData) {
        getRecommendationInteractor.execute(inputData);
    }

    public void executeBack() {
        gobackInteractor.execute();
    }

    /**
     * Executes the "switchToRecommendationMenu" Use Case.
     */
    void switchToRecommendationMenu() {
        getRecommendationInteractor.switchToRecommendationMenu();
    }

}


