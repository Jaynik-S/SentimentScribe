package com.sentimentscribe.usecase.save_entry;

import java.util.List;

public interface SaveEntryKeywordExtractor {
    List<String> extractKeywords(String textBody) throws Exception;
}


