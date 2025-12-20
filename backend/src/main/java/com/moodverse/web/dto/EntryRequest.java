package com.moodverse.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EntryRequest(
        String title,
        String text,
        String storagePath,
        List<String> keywords,
        LocalDateTime createdAt
) {
}
