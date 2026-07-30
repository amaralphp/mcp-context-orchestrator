package com.mcporchestrator.infrastructure.adapter.rest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mcporchestrator.application.port.input.IngestResourceUseCase;
import com.mcporchestrator.application.port.input.QueryUseCase;
import com.mcporchestrator.application.port.output.ResourceRepository;
import com.mcporchestrator.domain.entity.ContextQuery;
import com.mcporchestrator.domain.entity.MCPResource;
import com.mcporchestrator.domain.entity.QueryResult;
import com.mcporchestrator.infrastructure.adapter.rest.dto.IngestRequest;
import com.mcporchestrator.infrastructure.adapter.rest.dto.IngestResponse;
import com.mcporchestrator.infrastructure.adapter.rest.dto.QueryRequest;
import com.mcporchestrator.infrastructure.adapter.rest.dto.QueryResponse;

@RestController
@RequestMapping("/api/mcp")
public class MCPController {

    private static final Logger log = LoggerFactory.getLogger(MCPController.class);

    private final QueryUseCase queryUseCase;
    private final IngestResourceUseCase ingestResourceUseCase;
    private final ResourceRepository resourceRepository;

    public MCPController(
            QueryUseCase queryUseCase,
            IngestResourceUseCase ingestResourceUseCase,
            ResourceRepository resourceRepository
    ) {
        this.queryUseCase = queryUseCase;
        this.ingestResourceUseCase = ingestResourceUseCase;
        this.resourceRepository = resourceRepository;
    }

    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QueryResponse> query(@Valid @RequestBody QueryRequest request) {
        long startTime = System.currentTimeMillis();

        ContextQuery contextQuery = new ContextQuery(
                request.query(),
                request.sources(),
                request.maxTokens(),
                request.filters()
        );

        List<QueryResult> results = queryUseCase.executeQuery(contextQuery);
        String context = queryUseCase.buildContextString(contextQuery);

        long durationMs = System.currentTimeMillis() - startTime;

        List<QueryResponse.ResultItem> resultItems = results.stream()
                .map(r -> new QueryResponse.ResultItem(
                        r.resource().id().toString(),
                        r.resource().name(),
                        r.resource().type(),
                        r.resource().source(),
                        r.relevanceScore(),
                        r.excerpt()
                ))
                .toList();

        QueryResponse response = new QueryResponse(
                context,
                results.size(),
                resultItems,
                durationMs
        );

        log.info("Query '{}' returned {} results in {}ms",
                request.query(), results.size(), durationMs);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/ingest", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IngestResponse> ingest(@Valid @RequestBody IngestRequest request) {
        MCPResource resource = new MCPResource(
                UUID.randomUUID(),
                request.name(),
                request.type(),
                request.content(),
                request.metadata(),
                request.source(),
                Instant.now(),
                request.ttlSeconds()
        );

        MCPResource saved = ingestResourceUseCase.ingest(resource);

        IngestResponse response = new IngestResponse(
                saved.id().toString(),
                saved.name(),
                saved.type(),
                saved.source(),
                saved.createdAt(),
                true
        );

        log.info("Resource ingested: {} ({})", saved.id(), saved.name());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/resources/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MCPResource> getResource(@PathVariable("id") UUID id) {
        Optional<MCPResource> resource = resourceRepository.findById(id);
        return resource.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
