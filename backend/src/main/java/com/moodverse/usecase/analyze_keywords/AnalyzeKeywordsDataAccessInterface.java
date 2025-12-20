package com.moodverse.usecase.analyze_keywords;

import com.moodverse.domain.AnalysisResult;

public interface AnalyzeKeywordsDataAccessInterface {
    AnalysisResult analyze(String textBody);
}

