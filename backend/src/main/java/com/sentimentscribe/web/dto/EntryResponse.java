package com.sentimentscribe.web.dto;

import java.time.LocalDateTime;
public record EntryResponse(
        String storagePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String titleCiphertext,
        String titleIv,
        String bodyCiphertext,
        String bodyIv,
        String algo,
        int version
) {
}
