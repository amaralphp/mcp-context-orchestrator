package com.mcporchestrator.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

import org.springframework.stereotype.Service;

import com.mcporchestrator.domain.entity.MCPResource;
import com.mcporchestrator.domain.entity.QueryResult;

@Service
public class ContextBuilderService {

    public List<QueryResult> rankByRelevance(String query, List<MCPResource> resources) {
        List<QueryResult> results = new ArrayList<>();
        for (MCPResource resource : resources) {
            double score = computeRelevance(query, resource);
            if (score > 0.0) {
                String excerpt = buildExcerpt(resource.content(), 200);
                results.add(new QueryResult(resource, score, excerpt));
            }
        }
        results.sort(Comparator.comparingDouble(QueryResult::relevanceScore).reversed());
        return results;
    }

    public String buildContext(List<QueryResult> results, int maxTokens) {
        StringJoiner joiner = new StringJoiner("\n\n---\n\n");
        int totalChars = 0;
        int charBudget = maxTokens * 4;

        for (QueryResult result : results) {
            String block = formatResultBlock(result);
            if (totalChars + block.length() > charBudget) {
                int remaining = charBudget - totalChars;
                if (remaining > 50) {
                    joiner.add(block.substring(0, remaining) + "... [truncated]");
                }
                break;
            }
            joiner.add(block);
            totalChars += block.length();
        }
        return joiner.toString();
    }

    private double computeRelevance(String query, MCPResource resource) {
        String lowerQuery = query.toLowerCase();
        String lowerName = resource.name().toLowerCase();
        String lowerContent = resource.content().toLowerCase();
        String lowerType = resource.type().toLowerCase();

        double score = 0.0;

        if (lowerName.contains(lowerQuery)) {
            score += 0.5;
        }
        if (lowerContent.contains(lowerQuery)) {
            score += 0.3;
        }
        if (lowerType.contains(lowerQuery)) {
            score += 0.1;
        }

        String[] queryTerms = lowerQuery.split("\\s+");
        if (queryTerms.length > 1) {
            long matchCount = 0;
            for (String term : queryTerms) {
                if (lowerContent.contains(term)) {
                    matchCount++;
                }
            }
            score += 0.2 * ((double) matchCount / queryTerms.length);
        }

        if (resource.metadata() != null && !resource.metadata().isEmpty()) {
            long metaMatches = resource.metadata().values().stream()
                    .filter(v -> v != null && v.toLowerCase().contains(lowerQuery))
                    .count();
            score += 0.1 * ((double) metaMatches / resource.metadata().size());
        }

        return Math.min(1.0, score);
    }

    private String buildExcerpt(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength).replaceAll("\\s+", " ").trim() + "...";
    }

    private String formatResultBlock(QueryResult result) {
        MCPResource r = result.resource();
        return "[Source: %s | Type: %s | Score: %.2f]\n%s:\n%s"
                .formatted(r.source(), r.type(), result.relevanceScore(), r.name(), r.content());
    }
}
