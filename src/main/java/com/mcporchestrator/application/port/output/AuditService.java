package com.mcporchestrator.application.port.output;

import java.util.List;

import com.mcporchestrator.domain.entity.AuditEntry;

public interface AuditService {
    void record(AuditEntry entry);
    List<AuditEntry> getRecentAudits(int limit);
}
