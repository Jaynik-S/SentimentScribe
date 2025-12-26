package com.sentimentscribe.data;

import com.sentimentscribe.usecase.delete_entry.DeleteEntryUserDataAccessInterface;
import com.sentimentscribe.usecase.load_entry.LoadEntryUserDataAccessInterface;
import com.sentimentscribe.usecase.save_entry.SaveEntryUserDataAccessInterface;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DiaryEntryRepository extends SaveEntryUserDataAccessInterface,
        LoadEntryUserDataAccessInterface,
        DeleteEntryUserDataAccessInterface {
    List<Map<String, Object>> getAll(UUID userId) throws Exception;
}
