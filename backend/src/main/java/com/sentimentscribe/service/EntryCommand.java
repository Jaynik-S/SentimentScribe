package com.sentimentscribe.service;

import java.time.LocalDateTime;
import java.util.List;

public record EntryCommand(
        String title,
        String text,
        String storagePath,
        List<String> keywords,
        LocalDateTime createdAt
) {
}
