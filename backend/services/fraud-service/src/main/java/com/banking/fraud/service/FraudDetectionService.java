package com.banking.fraud.service;

import com.banking.fraud.entity.FraudAlert;
import com.banking.fraud.entity.FraudAlertStatus;
import com.banking.fraud.entity.FraudRuleType;
import com.banking.fraud.event.FraudAlertRaisedEvent;
import com.banking.fraud.event.TransactionCreatedEvent;
import com.banking.fraud.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private static final String FRAUD_ALERTS_TOPIC = "fraud-alerts";

    private final FraudAlertRepository fraudAlertRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${fraud.large-transaction-threshold:10000}")
    private BigDecimal largeTransactionThreshold;

    @Value("${fraud.rapid-transfer-window-minutes:5}")
    private int rapidTransferWindowMinutes;

    @Value("${fraud.rapid-transfer-max-count:3}")
    private int rapidTransferMaxCount;

    @Transactional
    public void analyzeTransaction(TransactionCreatedEvent event) {
        log.debug("Analyzing transaction [{}] for fraud", event.getTransactionReference());

        List<FraudRuleType> triggeredRules = new ArrayList<>();

        // Rule 1: Large transaction amount
        if (event.getAmount().compareTo(largeTransactionThreshold) >= 0) {
            triggeredRules.add(FraudRuleType.LARGE_TRANSACTION);
            log.warn("Large transaction detected: [{}] amount [{}]",
                    event.getTransactionReference(), event.getAmount());
        }

        // Rule 2: Rapid repeated transfers in short window
        if ("TRANSFER".equals(event.getTransactionType())) {
            UUID userId = UUID.fromString(event.getUserId());
            LocalDateTime since = LocalDateTime.now().minusMinutes(rapidTransferWindowMinutes);
            long recentCount = fraudAlertRepository.countRecentAlertsByUser(userId, since);
            if (recentCount >= rapidTransferMaxCount) {
                triggeredRules.add(FraudRuleType.RAPID_REPEATED_TRANSFERS);
                log.warn("Rapid repeated transfers detected for user [{}]: {} in {} minutes",
                        event.getUserId(), recentCount, rapidTransferWindowMinutes);
            }
        }

        for (FraudRuleType rule : triggeredRules) {
            createAndPublishAlert(event, rule);
        }
    }

    private void createAndPublishAlert(TransactionCreatedEvent event, FraudRuleType ruleType) {
        FraudAlert alert = new FraudAlert();
        alert.setUserId(UUID.fromString(event.getUserId()));
        alert.setTransactionReference(event.getTransactionReference());
        alert.setRuleTriggered(ruleType);
        alert.setStatus(FraudAlertStatus.OPEN);
        alert.setDescription(buildDescription(ruleType, event));
        alert.setTransactionAmount(event.getAmount());
        alert.setAccountNumber(event.getSourceAccountNumber() != null
                ? event.getSourceAccountNumber() : event.getDestinationAccountNumber());
        alert = fraudAlertRepository.save(alert);

        FraudAlertRaisedEvent alertEvent = FraudAlertRaisedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .fraudAlertId(alert.getId().toString())
                .transactionReference(event.getTransactionReference())
                .userId(event.getUserId())
                .ruleType(ruleType.name())
                .amount(event.getAmount())
                .description(alert.getDescription())
                .occurredAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(FRAUD_ALERTS_TOPIC, event.getTransactionReference(), alertEvent);
        log.info("Fraud alert created and published: alert [{}] rule [{}] transaction [{}]",
                alert.getId(), ruleType, event.getTransactionReference());
    }

    private String buildDescription(FraudRuleType rule, TransactionCreatedEvent event) {
        return switch (rule) {
            case LARGE_TRANSACTION -> String.format(
                    "Large transaction of %s %s detected on transaction %s",
                    event.getAmount(), event.getCurrency(), event.getTransactionReference());
            case RAPID_REPEATED_TRANSFERS -> String.format(
                    "Rapid repeated transfers: more than %d transfers in %d minutes for user %s",
                    rapidTransferMaxCount, rapidTransferWindowMinutes, event.getUserId());
            case NEW_BENEFICIARY_TRANSFER -> String.format(
                    "Transfer to potentially new beneficiary on transaction %s",
                    event.getTransactionReference());
            default -> String.format("Fraud rule %s triggered for transaction %s",
                    rule, event.getTransactionReference());
        };
    }
}
