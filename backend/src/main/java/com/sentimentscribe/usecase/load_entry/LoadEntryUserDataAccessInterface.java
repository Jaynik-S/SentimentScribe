package com.sentimentscribe.usecase.load_entry;

import com.sentimentscribe.domain.DiaryEntry;
import java.util.UUID;

public interface LoadEntryUserDataAccessInterface {
    DiaryEntry getByPath(UUID userId, String entryPath) throws Exception;
}

