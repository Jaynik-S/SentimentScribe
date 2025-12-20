package com.moodverse.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EntrySummaryResponse(
        String title,
        String storagePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> keywords
) {
}
