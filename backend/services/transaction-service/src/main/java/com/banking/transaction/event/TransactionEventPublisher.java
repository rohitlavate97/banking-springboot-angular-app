package com.banking.transaction.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction-completed";
    private static final String TRANSACTION_FAILED_TOPIC = "transaction-failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransactionCreated(TransactionCreatedEvent event) {
        publishEvent(TRANSACTION_EVENTS_TOPIC, event.getTransactionReference(), event);
    }

    public void publishTransactionCompleted(TransactionCompletedEvent event) {
        publishEvent(TRANSACTION_COMPLETED_TOPIC, event.getTransactionReference(), event);
    }

    public void publishTransactionFailed(TransactionFailedEvent event) {
        publishEvent(TRANSACTION_FAILED_TOPIC, event.getTransactionReference(), event);
    }

    private void publishEvent(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Event published to topic [{}] partition [{}] offset [{}]",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event to topic [{}]: {}", topic, ex.getMessage(), ex);
            }
        });
    }
}
