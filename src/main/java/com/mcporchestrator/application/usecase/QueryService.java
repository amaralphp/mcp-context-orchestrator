package com.mcporchestrator.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.mcporchestrator.application.event.QueryExecutedEvent;
import com.mcporchestrator.application.port.input.QueryUseCase;
import com.mcporchestrator.application.port.output.AuditService;
import com.mcporchestrator.application.port.output.CacheService;
import com.mcporchestrator.application.port.output.ResourceRepository;
import com.mcporchestrator.domain.entity.AuditEntry;
import com.mcporchestrator.domain.entity.ContextQuery;
import com.mcporchestrator.domain.entity.MCPResource;
import com.mcporchestrator.domain.entity.QueryResult;
import com.mcporchestrator.domain.service.ContextBuilderService;

@Service
public class QueryService implements QueryUseCase {

    private final ResourceRepository resourceRepository;
    private final ContextBuilderService contextBuilderService;
    private final CacheService cacheService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public QueryService(
            ResourceRepository resourceRepository,
            ContextBuilderService contextBuilderService,
            CacheService cacheService,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.resourceRepository = resourceRepository;
        this.contextBuilderService = contextBuilderService;
        this.cacheService = cacheService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<QueryResult> executeQuery(ContextQuery contextQuery) {
        long startTime = System.currentTimeMillis();

        String cacheKey = buildCacheKey(contextQuery);
        Optional<List<MCPResource>> cached = cacheService.getSearchResults(cacheKey);
        List<MCPResource> resources;

        if (cached.isPresent()) {
            resources = cached.get();
        } else {
            resources = searchSources(contextQuery);
            cacheService.cacheSearchResults(cacheKey, resources);
        }

        List<QueryResult> ranked = contextBuilderService.rankByRelevance(
                contextQuery.query(), resources
        );

        long duration = System.currentTimeMillis() - startTime;
        auditQuery(contextQuery, ranked.size(), duration);

        eventPublisher.publishEvent(new QueryExecutedEvent(
                contextQuery, ranked.size(), duration
        ));

        return ranked;
    }

    @Override
    public String buildContextString(ContextQuery contextQuery) {
        List<QueryResult> results = executeQuery(contextQuery);
        return contextBuilderService.buildContext(results, contextQuery.maxTokens());
    }

    private List<MCPResource> searchSources(ContextQuery query) {
        List<MCPResource> results = new ArrayList<>();
        String typeFilter = query.filters() != null ? query.filters().get("type") : null;

        List<MCPResource> fromRepo = resourceRepository.search(
                query.query(), typeFilter, null
        );
        results.addAll(fromRepo);

        if (results.isEmpty()) {
            results.addAll(resourceRepository.findAll());
        }

        return results;
    }

    private String buildCacheKey(ContextQuery query) {
        return "search:%s:%s:%s".formatted(
                query.query().toLowerCase().trim(),
                String.join(",", query.sources()),
                query.filters() != null ? query.filters().toString() : ""
        );
    }

    private void auditQuery(ContextQuery query, int resultCount, long durationMs) {
        AuditEntry entry = AuditEntry.forQuery(
                query.query(),
                String.join(",", query.sources()),
                resultCount,
                durationMs
        );
        auditService.record(entry);
    }
}
