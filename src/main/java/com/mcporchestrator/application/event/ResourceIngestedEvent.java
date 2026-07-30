package com.mcporchestrator.application.event;

import java.time.Instant;
import java.util.UUID;

import com.mcporchestrator.domain.entity.MCPResource;

public record ResourceIngestedEvent(
        UUID eventId,
        MCPResource resource,
        Instant occurredAt
) {
    public ResourceIngestedEvent(MCPResource resource) {
        this(UUID.randomUUID(), resource, Instant.now());
    }
}
