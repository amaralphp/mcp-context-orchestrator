package com.mcporchestrator.application.port.output;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mcporchestrator.domain.entity.MCPResource;

public interface CacheService {
    void cacheResource(MCPResource resource);
    Optional<MCPResource> getResource(UUID id);
    void invalidateResource(UUID id);
    void cacheSearchResults(String queryKey, List<MCPResource> results);
    Optional<List<MCPResource>> getSearchResults(String queryKey);
    void clearAll();
}
