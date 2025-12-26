package com.sentimentscribe.web.dto;

import java.time.LocalDateTime;
public record EntryRequest(
        String storagePath,
        LocalDateTime createdAt,
        String titleCiphertext,
        String titleIv,
        String bodyCiphertext,
        String bodyIv,
        String algo,
        int version
) {
}
