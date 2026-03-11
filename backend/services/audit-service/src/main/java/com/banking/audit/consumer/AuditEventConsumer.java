package com.banking.audit.consumer;

import com.banking.audit.entity.AuditEventType;
import com.banking.audit.entity.AuditLog;
import com.banking.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "${spring.kafka.consumer.group-id}-txn-created",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onTransactionCreated(@Payload Map<String, Object> event,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      Acknowledgment acknowledgment) {
        auditAndAck(event, AuditEventType.TRANSACTION_CREATED, "TRANSACTION",
                getStr(event, "transactionReference"), acknowledgment);
    }

    @KafkaListener(
            topics = "transaction-completed",
            groupId = "${spring.kafka.consumer.group-id}-txn-completed",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onTransactionCompleted(@Payload Map<String, Object> event,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        Acknowledgment acknowledgment) {
        auditAndAck(event, AuditEventType.TRANSACTION_COMPLETED, "TRANSACTION",
                getStr(event, "transactionReference"), acknowledgment);
    }

    @KafkaListener(
            topics = "transaction-failed",
            groupId = "${spring.kafka.consumer.group-id}-txn-failed",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onTransactionFailed(@Payload Map<String, Object> event,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     Acknowledgment acknowledgment) {
        auditAndAck(event, AuditEventType.TRANSACTION_FAILED, "TRANSACTION",
                getStr(event, "transactionReference"), acknowledgment);
    }

    @KafkaListener(
            topics = "fraud-alerts",
            groupId = "${spring.kafka.consumer.group-id}-fraud",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onFraudAlert(@Payload Map<String, Object> event,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                              Acknowledgment acknowledgment) {
        auditAndAck(event, AuditEventType.FRAUD_ALERT_CREATED, "FRAUD_ALERT",
                getStr(event, "fraudAlertId"), acknowledgment);
    }

    private void auditAndAck(Map<String, Object> event, AuditEventType eventType,
                              String entityType, String entityId, Acknowledgment acknowledgment) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            AuditLog log = new AuditLog();
            log.setUserId(getStr(event, "userId"));
            log.setEventType(eventType);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setEventData(eventData);
            log.setCorrelationId(getStr(event, "correlationId"));
            auditLogRepository.save(log);
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            AuditEventConsumer.log.error("Failed to audit event [{}] for entity [{}]: {}",
                    eventType, entityId, ex.getMessage(), ex);
        }
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
