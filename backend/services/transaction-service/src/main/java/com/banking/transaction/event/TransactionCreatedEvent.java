package com.banking.transaction.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedEvent {
    private String eventId;
    private String transactionReference;
    private UUID userId;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String correlationId;
    private LocalDateTime occurredAt;
}
