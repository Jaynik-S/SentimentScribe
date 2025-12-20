package com.moodverse.service;

import com.moodverse.usecase.get_recommendations.GetRecommendationsInputData;
import com.moodverse.usecase.get_recommendations.GetRecommendationsInteractor;
import com.moodverse.usecase.get_recommendations.GetRecommendationsOutputBoundary;
import com.moodverse.usecase.get_recommendations.GetRecommendationsOutputData;
import com.moodverse.usecase.get_recommendations.GetRecommendationsUserDataAccessInterface;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private final GetRecommendationsUserDataAccessInterface recommendationsAccess;

    public RecommendationService(GetRecommendationsUserDataAccessInterface recommendationsAccess) {
        this.recommendationsAccess = recommendationsAccess;
    }

    public ServiceResult<GetRecommendationsOutputData> recommend(String text) {
        RecommendationPresenter presenter = new RecommendationPresenter();
        GetRecommendationsInteractor interactor = new GetRecommendationsInteractor(recommendationsAccess, presenter);
        interactor.execute(new GetRecommendationsInputData(text));
        if (presenter.errorMessage != null) {
            return ServiceResult.failure(presenter.errorMessage);
        }
        return ServiceResult.success(presenter.outputData);
    }

    private static final class RecommendationPresenter implements GetRecommendationsOutputBoundary {
        private GetRecommendationsOutputData outputData;
        private String errorMessage;

        @Override
        public void prepareSuccessView(GetRecommendationsOutputData outputData) {
            this.outputData = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public void switchToRecommendationMenu() {
            // No-op for API response flow.
        }
    }
}
