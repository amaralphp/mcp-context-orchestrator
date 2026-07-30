package com.mcporchestrator.domain.entity;

import java.util.List;
import java.util.Map;

public record ContextQuery(
        String query,
        List<String> sources,
        int maxTokens,
        Map<String, String> filters
) {
    public ContextQuery {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be blank");
        }
        if (maxTokens < 1) {
            throw new IllegalArgumentException("maxTokens must be at least 1");
        }
        if (sources == null) {
            sources = List.of();
        }
        if (filters == null) {
            filters = Map.of();
        }
    }

    public ContextQuery withSource(String source) {
        return new ContextQuery(query, List.of(source), maxTokens, filters);
    }

    public boolean targetsSource(String sourceName) {
        return sources.isEmpty() || sources.contains(sourceName);
    }
}
