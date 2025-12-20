package com.moodverse.usecase.save_entry;

import com.moodverse.domain.DiaryEntry;

public interface SaveEntryUserDataAccessInterface {
    boolean save(DiaryEntry entry) throws Exception;
}


