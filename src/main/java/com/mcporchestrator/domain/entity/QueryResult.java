package com.mcporchestrator.domain.entity;

public record QueryResult(
        MCPResource resource,
        double relevanceScore,
        String excerpt
) {
    public QueryResult {
        if (resource == null) {
            throw new IllegalArgumentException("Resource must not be null");
        }
        if (relevanceScore < 0.0 || relevanceScore > 1.0) {
            throw new IllegalArgumentException("Relevance score must be between 0.0 and 1.0");
        }
        if (excerpt == null) {
            excerpt = "";
        }
    }

    public String truncatedExcerpt(int maxLength) {
        if (excerpt.length() <= maxLength) {
            return excerpt;
        }
        return excerpt.substring(0, maxLength) + "...";
    }
}
