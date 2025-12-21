package com.sentimentscribe.usecase.analyze_keywords;

public interface AnalyzeKeywordsOutputBoundary {
    void prepareSuccessView(AnalyzeKeywordsOutputData outputData);

    void prepareFailView(String errorMessage);
}

