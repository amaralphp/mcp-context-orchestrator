package com.mcporchestrator.infrastructure.adapter.rest.dto;

import java.time.Instant;

public record IngestResponse(
        String id,
        String name,
        String type,
        String source,
        Instant createdAt,
        boolean success
) {}
