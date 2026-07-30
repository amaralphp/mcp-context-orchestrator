package com.mcporchestrator.domain.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MCPResource(
        UUID id,
        String name,
        String type,
        String content,
        Map<String, String> metadata,
        String source,
        Instant createdAt,
        long ttlSeconds
) {
    public MCPResource {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Resource name must not be blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Resource type must not be blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Resource content must not be blank");
        }
        if (ttlSeconds < 0) {
            throw new IllegalArgumentException("TTL must be non-negative");
        }
    }

    public MCPResource withId(UUID newId) {
        return new MCPResource(newId, name, type, content, metadata, source, createdAt, ttlSeconds);
    }

    public MCPResource withTimestamp(Instant timestamp) {
        return new MCPResource(id, name, type, content, metadata, source, timestamp, ttlSeconds);
    }

    public boolean isExpired() {
        return createdAt != null
                && ttlSeconds > 0
                && Instant.now().isAfter(createdAt.plusSeconds(ttlSeconds));
    }
}
