package com.mcporchestrator.application.event;

import java.time.Instant;
import java.util.UUID;

import com.mcporchestrator.domain.entity.ContextQuery;

public record QueryExecutedEvent(
        UUID eventId,
        ContextQuery query,
        int resultCount,
        long durationMs,
        Instant occurredAt
) {
    public QueryExecutedEvent(ContextQuery query, int resultCount, long durationMs) {
        this(UUID.randomUUID(), query, resultCount, durationMs, Instant.now());
    }
}
