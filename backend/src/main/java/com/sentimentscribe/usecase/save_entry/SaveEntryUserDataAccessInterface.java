package com.sentimentscribe.usecase.save_entry;

import com.sentimentscribe.domain.DiaryEntry;

public interface SaveEntryUserDataAccessInterface {
    boolean save(DiaryEntry entry) throws Exception;
}


