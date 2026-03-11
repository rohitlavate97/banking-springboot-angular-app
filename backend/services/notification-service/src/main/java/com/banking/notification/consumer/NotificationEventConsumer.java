package com.banking.notification.consumer;

import com.banking.notification.event.FraudAlertRaisedEvent;
import com.banking.notification.event.TransactionCompletedEvent;
import com.banking.notification.event.TransactionFailedEvent;
import com.banking.notification.service.NotificationService;
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
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "transaction-completed",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "transactionCompletedListenerFactory"
    )
    public void onTransactionCompleted(@Payload TransactionCompletedEvent event,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        Acknowledgment acknowledgment) {
        log.info("Received transaction-completed event [{}]", event.getTransactionReference());
        try {
            // In production, look up user email from user-service; using userId as placeholder here
            String recipientEmail = event.getUserId() + "@banking.internal";
            notificationService.sendTransactionSuccessNotification(
                    event.getUserId(),
                    recipientEmail,
                    event.getTransactionReference(),
                    event.getAmount().toPlainString(),
                    event.getCurrency()
            );
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process transaction-completed event [{}]: {}",
                    event.getTransactionReference(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics = "transaction-failed",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "transactionFailedListenerFactory"
    )
    public void onTransactionFailed(@Payload TransactionFailedEvent event,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     Acknowledgment acknowledgment) {
        log.info("Received transaction-failed event [{}]", event.getTransactionReference());
        try {
            String recipientEmail = event.getUserId() + "@banking.internal";
            notificationService.sendTransactionFailedNotification(
                    event.getUserId(),
                    recipientEmail,
                    event.getTransactionReference(),
                    event.getFailureReason()
            );
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process transaction-failed event [{}]: {}",
                    event.getTransactionReference(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics = "fraud-alerts",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "fraudAlertListenerFactory"
    )
    public void onFraudAlert(@Payload FraudAlertRaisedEvent event,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                              Acknowledgment acknowledgment) {
        log.info("Received fraud-alert event for transaction [{}]", event.getTransactionReference());
        try {
            String recipientEmail = event.getUserId() + "@banking.internal";
            notificationService.sendFraudAlertNotification(
                    event.getUserId(),
                    recipientEmail,
                    event.getTransactionReference(),
                    event.getDescription()
            );
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process fraud-alert event [{}]: {}",
                    event.getTransactionReference(), ex.getMessage(), ex);
        }
    }
}
