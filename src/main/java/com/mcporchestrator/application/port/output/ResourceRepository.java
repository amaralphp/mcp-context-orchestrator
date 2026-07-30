package com.mcporchestrator.application.port.output;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mcporchestrator.domain.entity.MCPResource;

public interface ResourceRepository {
    MCPResource save(MCPResource resource);
    Optional<MCPResource> findById(UUID id);
    List<MCPResource> search(String query, String type, String source);
    List<MCPResource> findAll();
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
