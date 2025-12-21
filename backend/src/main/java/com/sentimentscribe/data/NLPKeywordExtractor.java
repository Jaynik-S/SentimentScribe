package com.sentimentscribe.data;

import com.sentimentscribe.domain.Keyword;
import com.sentimentscribe.usecase.save_entry.SaveEntryKeywordExtractor;

import java.util.List;

public class NLPKeywordExtractor implements SaveEntryKeywordExtractor {

    private final NLPAnalysisDataAccessObject analysisDao;

    public NLPKeywordExtractor(NLPAnalysisDataAccessObject analysisDao) {
        this.analysisDao = analysisDao;
    }

    @Override
    public List<String> extractKeywords(String textBody) {
        return analysisDao.analyze(textBody)
                .keywords()
                .stream()
                .map(Keyword::text)
                .toList();
    }
}


