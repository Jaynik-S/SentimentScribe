package com.sentimentscribe.persistence.postgres;

import java.util.UUID;

public class StoragePathGenerator {
    public String generate() {
        return "db:" + UUID.randomUUID();
    }
}
