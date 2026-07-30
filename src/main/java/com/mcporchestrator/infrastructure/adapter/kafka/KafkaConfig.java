package com.mcporchestrator.infrastructure.adapter.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Value("${mcp.kafka.audit-topic:audit-events}")
    private String auditTopic;

    @Value("${mcp.kafka.partitions:3}")
    private int partitions;

    @Value("${mcp.kafka.replication-factor:1}")
    private short replicationFactor;

    @Bean
    public NewTopic auditTopic() {
        return TopicBuilder.name(auditTopic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .compact()
                .build();
    }

    @Bean
    public KafkaAdmin kafkaAdmin(ProducerFactory<String, Object> producerFactory) {
        return new KafkaAdmin(producerFactory.getConfigurationProperties());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.setRecordMessageConverter(new StringJsonMessageConverter());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(1000L, 3)
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    @Bean
    public StringJsonMessageConverter jsonMessageConverter() {
        return new StringJsonMessageConverter();
    }
}
