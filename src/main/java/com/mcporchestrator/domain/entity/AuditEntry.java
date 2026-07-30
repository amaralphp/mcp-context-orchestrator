package com.mcporchestrator.domain.entity;

import java.time.Instant;
import java.util.UUID;

public record AuditEntry(
        UUID id,
        String query,
        String sourceUsed,
        int resultCount,
        long durationMs,
        Instant timestamp
) {
    public AuditEntry {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be blank");
        }
        if (sourceUsed == null || sourceUsed.isBlank()) {
            throw new IllegalArgumentException("Source must not be blank");
        }
        if (resultCount < 0) {
            throw new IllegalArgumentException("Result count must be non-negative");
        }
        if (durationMs < 0) {
            throw new IllegalArgumentException("Duration must be non-negative");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static AuditEntry forQuery(String query, String source, int count, long durationMs) {
        return new AuditEntry(UUID.randomUUID(), query, source, count, durationMs, Instant.now());
    }
}
