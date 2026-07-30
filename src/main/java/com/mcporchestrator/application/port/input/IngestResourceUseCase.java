package com.mcporchestrator.application.port.input;

import com.mcporchestrator.domain.entity.MCPResource;

public interface IngestResourceUseCase {
    MCPResource ingest(MCPResource resource);
}
