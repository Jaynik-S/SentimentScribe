package com.sentimentscribe.usecase.analyze_keywords;

import com.sentimentscribe.domain.AnalysisResult;

public interface AnalyzeKeywordsDataAccessInterface {
    AnalysisResult analyze(String textBody);
}

