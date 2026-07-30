package com.mcporchestrator.infrastructure.adapter.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.mcporchestrator.application.port.output.AuditService;
import com.mcporchestrator.domain.entity.AuditEntry;

@Component
public class KafkaAuditProducer implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAuditProducer.class);
    private static final int MAX_RECENT_AUDITS = 100;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String auditTopic;
    private final List<AuditEntry> recentAudits = new ArrayList<>();

    public KafkaAuditProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${mcp.kafka.audit-topic:audit-events}") String auditTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.auditTopic = auditTopic;
    }

    @Override
    public void record(AuditEntry entry) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(auditTopic, entry.id().toString(), entry);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send audit entry to Kafka: {}", entry.id(), ex);
            } else {
                log.debug("Audit entry sent to Kafka: {} at offset {}",
                        entry.id(), result.getRecordMetadata().offset());
            }
        });

        synchronized (recentAudits) {
            recentAudits.add(entry);
            if (recentAudits.size() > MAX_RECENT_AUDITS) {
                recentAudits.remove(0);
            }
        }
    }

    @Override
    public List<AuditEntry> getRecentAudits(int limit) {
        synchronized (recentAudits) {
            int fromIndex = Math.max(0, recentAudits.size() - limit);
            return new ArrayList<>(recentAudits.subList(fromIndex, recentAudits.size()));
        }
    }
}
