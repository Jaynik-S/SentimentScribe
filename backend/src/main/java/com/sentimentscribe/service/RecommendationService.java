package com.sentimentscribe.service;

import com.sentimentscribe.usecase.get_recommendations.GetRecommendationsInputData;
import com.sentimentscribe.usecase.get_recommendations.GetRecommendationsInteractor;
import com.sentimentscribe.usecase.get_recommendations.GetRecommendationsOutputBoundary;
import com.sentimentscribe.usecase.get_recommendations.GetRecommendationsOutputData;
import com.sentimentscribe.usecase.get_recommendations.GetRecommendationsUserDataAccessInterface;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private final GetRecommendationsUserDataAccessInterface recommendationsAccess;

    public RecommendationService(GetRecommendationsUserDataAccessInterface recommendationsAccess) {
        this.recommendationsAccess = recommendationsAccess;
    }

    public ServiceResult<GetRecommendationsOutputData> recommend(String text) {
        return recommend(text, java.util.List.of(), java.util.List.of());
    }

    public ServiceResult<GetRecommendationsOutputData> recommend(
            String text,
            java.util.List<String> excludeSongIds,
            java.util.List<String> excludeMovieIds
    ) {
        RecommendationPresenter presenter = new RecommendationPresenter();
        GetRecommendationsInteractor interactor = new GetRecommendationsInteractor(recommendationsAccess, presenter);
        interactor.execute(new GetRecommendationsInputData(text, excludeSongIds, excludeMovieIds));
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
