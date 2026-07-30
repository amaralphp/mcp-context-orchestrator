package com.mcporchestrator.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mcporchestrator.application.event.ResourceIngestedEvent;
import com.mcporchestrator.application.port.input.IngestResourceUseCase;
import com.mcporchestrator.application.port.output.AuditService;
import com.mcporchestrator.application.port.output.CacheService;
import com.mcporchestrator.application.port.output.ResourceRepository;
import com.mcporchestrator.domain.entity.AuditEntry;
import com.mcporchestrator.domain.entity.MCPResource;

@Service
public class IngestResourceService implements IngestResourceUseCase {

    private final ResourceRepository resourceRepository;
    private final CacheService cacheService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public IngestResourceService(
            ResourceRepository resourceRepository,
            CacheService cacheService,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.resourceRepository = resourceRepository;
        this.cacheService = cacheService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public MCPResource ingest(MCPResource resource) {
        MCPResource toSave = resource;
        if (toSave.id() == null) {
            toSave = toSave.withId(UUID.randomUUID());
        }
        if (toSave.createdAt() == null) {
            toSave = toSave.withTimestamp(Instant.now());
        }

        MCPResource saved = resourceRepository.save(toSave);

        cacheService.cacheResource(saved);

        auditService.record(AuditEntry.forQuery(
                "INGEST:" + saved.name(), saved.source(), 1, 0
        ));

        eventPublisher.publishEvent(new ResourceIngestedEvent(saved));

        return saved;
    }
}
