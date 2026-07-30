package com.mcporchestrator.infrastructure.adapter.rest.dto;

import java.util.List;

public record QueryResponse(
        String context,
        int totalResults,
        List<ResultItem> results,
        long durationMs
) {
    public record ResultItem(
            String resourceId,
            String resourceName,
            String resourceType,
            String source,
            double relevanceScore,
            String excerpt
    ) {}
}
