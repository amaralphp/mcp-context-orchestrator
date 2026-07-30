package com.mcporchestrator.infrastructure.adapter.rest.dto;

import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record IngestRequest(
        @NotBlank(message = "Resource name must not be blank")
        String name,
        @NotBlank(message = "Resource type must not be blank")
        String type,
        @NotBlank(message = "Resource content must not be blank")
        String content,
        Map<String, String> metadata,
        String source,
        @Min(value = 0, message = "TTL must be non-negative")
        long ttlSeconds
) {
    public IngestRequest {
        if (metadata == null) {
            metadata = Map.of();
        }
        if (source == null) {
            source = "unknown";
        }
        if (ttlSeconds < 0) {
            ttlSeconds = 0;
        }
    }
}
