package com.sentimentscribe.usecase.get_recommendations;

public interface GetRecommendationsOutputBoundary {
    void prepareSuccessView(GetRecommendationsOutputData outputData);

    void prepareFailView(String error);

    void switchToRecommendationMenu();

}
