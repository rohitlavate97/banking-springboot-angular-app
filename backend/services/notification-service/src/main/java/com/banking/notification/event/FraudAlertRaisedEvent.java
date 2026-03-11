package com.banking.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertRaisedEvent {
    private String eventId;
    private String fraudAlertId;
    private String transactionReference;
    private String userId;
    private String ruleType;
    private BigDecimal amount;
    private String description;
    private LocalDateTime occurredAt;
}
