package com.moodverse.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EntryResponse(
        String title,
        String text,
        String storagePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> keywords
) {
}
