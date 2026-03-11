package com.banking.fraud.consumer;

import com.banking.fraud.event.TransactionCreatedEvent;
import com.banking.fraud.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final FraudDetectionService fraudDetectionService;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionCreated(@Payload TransactionCreatedEvent event,
                                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                          @Header(KafkaHeaders.OFFSET) long offset,
                                          Acknowledgment acknowledgment) {
        log.info("Received transaction event [{}] from topic [{}] partition [{}] offset [{}]",
                event.getTransactionReference(), topic, partition, offset);
        try {
            fraudDetectionService.analyzeTransaction(event);
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process transaction event [{}]: {}",
                    event.getTransactionReference(), ex.getMessage(), ex);
            // Do not acknowledge — will be retried or sent to DLQ
        }
    }
}
