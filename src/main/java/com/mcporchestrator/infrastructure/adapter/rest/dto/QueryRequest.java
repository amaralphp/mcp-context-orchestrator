package com.mcporchestrator.infrastructure.adapter.rest.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record QueryRequest(
        @NotBlank(message = "Query must not be blank")
        String query,
        List<String> sources,
        @Min(value = 1, message = "maxTokens must be at least 1")
        int maxTokens,
        Map<String, String> filters
) {
    public QueryRequest {
        if (sources == null) {
            sources = List.of();
        }
        if (filters == null) {
            filters = Map.of();
        }
        if (maxTokens < 1) {
            maxTokens = 2000;
        }
    }
}
