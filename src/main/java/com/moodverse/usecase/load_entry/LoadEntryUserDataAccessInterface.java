package com.moodverse.usecase.load_entry;

import com.moodverse.domain.DiaryEntry;

public interface LoadEntryUserDataAccessInterface {
    DiaryEntry getByPath(String entryPath) throws Exception;
}

