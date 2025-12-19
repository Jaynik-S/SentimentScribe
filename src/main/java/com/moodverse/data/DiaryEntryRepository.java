package com.moodverse.data;

import com.moodverse.domain.DiaryEntry;

import java.util.List;
import java.util.Map;

public interface DiaryEntryRepository {
    boolean save(DiaryEntry entry) throws Exception;

    DiaryEntry getByPath(String entryPath) throws Exception;

    boolean deleteByPath(String entryPath) throws Exception;

    List<Map<String, Object>> getAll() throws Exception;
}
