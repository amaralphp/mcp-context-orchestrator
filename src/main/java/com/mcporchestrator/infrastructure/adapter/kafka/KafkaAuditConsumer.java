package com.mcporchestrator.infrastructure.adapter.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.mcporchestrator.domain.entity.AuditEntry;

@Component
public class KafkaAuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaAuditConsumer.class);

    @KafkaListener(
            topics = "${mcp.kafka.audit-topic:audit-events}",
            groupId = "${spring.kafka.consumer.group-id:mcp-context-orchestrator}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAuditEntry(@Payload AuditEntry entry) {
        log.info("Audit consumed - query: '{}', source: {}, results: {}, duration: {}ms",
                entry.query(), entry.sourceUsed(), entry.resultCount(), entry.durationMs());
    }
}
