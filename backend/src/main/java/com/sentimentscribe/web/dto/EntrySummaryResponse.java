package com.sentimentscribe.web.dto;

import java.time.LocalDateTime;
public record EntrySummaryResponse(
        String storagePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String titleCiphertext,
        String titleIv,
        String algo,
        int version
) {
}
