package com.sentimentscribe.usecase.save_entry;

import com.sentimentscribe.domain.DiaryEntry;
import java.util.UUID;

public interface SaveEntryUserDataAccessInterface {
    boolean save(UUID userId, DiaryEntry entry) throws Exception;
}


