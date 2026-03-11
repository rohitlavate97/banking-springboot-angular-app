package com.banking.transaction.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name("transaction-events")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionCompletedTopic() {
        return TopicBuilder.name("transaction-completed")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionFailedTopic() {
        return TopicBuilder.name("transaction-failed")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
