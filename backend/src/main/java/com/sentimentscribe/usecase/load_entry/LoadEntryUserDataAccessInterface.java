package com.sentimentscribe.usecase.load_entry;

import com.sentimentscribe.domain.DiaryEntry;

public interface LoadEntryUserDataAccessInterface {
    DiaryEntry getByPath(String entryPath) throws Exception;
}

