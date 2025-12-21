package com.sentimentscribe.service;

import com.sentimentscribe.usecase.analyze_keywords.AnalyzeKeywordsInputData;
import com.sentimentscribe.usecase.analyze_keywords.AnalyzeKeywordsInteractor;
import com.sentimentscribe.usecase.analyze_keywords.AnalyzeKeywordsOutputBoundary;
import com.sentimentscribe.usecase.analyze_keywords.AnalyzeKeywordsOutputData;
import com.sentimentscribe.usecase.analyze_keywords.AnalyzeKeywordsDataAccessInterface;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    private final AnalyzeKeywordsDataAccessInterface analysisDataAccess;

    public AnalysisService(AnalyzeKeywordsDataAccessInterface analysisDataAccess) {
        this.analysisDataAccess = analysisDataAccess;
    }

    public ServiceResult<AnalyzeKeywordsOutputData> analyze(String text) {
        AnalyzePresenter presenter = new AnalyzePresenter();
        AnalyzeKeywordsInteractor interactor = new AnalyzeKeywordsInteractor(analysisDataAccess, presenter);
        interactor.execute(new AnalyzeKeywordsInputData(text));
        if (presenter.errorMessage != null) {
            return ServiceResult.failure(presenter.errorMessage);
        }
        return ServiceResult.success(presenter.outputData);
    }

    private static final class AnalyzePresenter implements AnalyzeKeywordsOutputBoundary {
        private AnalyzeKeywordsOutputData outputData;
        private String errorMessage;

        @Override
        public void prepareSuccessView(AnalyzeKeywordsOutputData outputData) {
            this.outputData = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
