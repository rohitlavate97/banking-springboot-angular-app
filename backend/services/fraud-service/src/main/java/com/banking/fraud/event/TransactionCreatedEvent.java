package com.banking.fraud.event;

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
public class TransactionCreatedEvent {
    private String eventId;
    private String transactionReference;
    private String userId;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String correlationId;
    private LocalDateTime occurredAt;
}
