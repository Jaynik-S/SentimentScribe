package com.sentimentscribe.service;

import java.time.LocalDateTime;

public record EntryCommand(
        String titleCiphertext,
        String titleIv,
        String bodyCiphertext,
        String bodyIv,
        String algo,
        int version,
        String storagePath,
        LocalDateTime createdAt
) {
}
