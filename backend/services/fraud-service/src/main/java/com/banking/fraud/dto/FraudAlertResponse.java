package com.banking.fraud.dto;

import com.banking.fraud.entity.FraudAlertStatus;
import com.banking.fraud.entity.FraudRuleType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class FraudAlertResponse {
    private UUID id;
    private UUID userId;
    private String transactionReference;
    private FraudRuleType ruleType;
    private FraudAlertStatus status;
    private String description;
    private BigDecimal amount;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private LocalDateTime createdAt;
}
