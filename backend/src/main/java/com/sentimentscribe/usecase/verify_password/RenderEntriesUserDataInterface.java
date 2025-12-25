package com.sentimentscribe.usecase.verify_password;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RenderEntriesUserDataInterface {
    List<Map<String, Object>> getAll(UUID userId) throws Exception;
}

