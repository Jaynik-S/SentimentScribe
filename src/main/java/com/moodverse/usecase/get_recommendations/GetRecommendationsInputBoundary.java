package com.moodverse.usecase.get_recommendations;

public interface GetRecommendationsInputBoundary {
    void execute(GetRecommendationsInputData inputData);

    void switchToRecommendationMenu();

}


