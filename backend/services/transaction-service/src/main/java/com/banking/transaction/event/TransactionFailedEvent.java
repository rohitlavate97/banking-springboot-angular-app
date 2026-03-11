package com.banking.transaction.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFailedEvent {
    private String eventId;
    private String transactionReference;
    private UUID userId;
    private String sourceAccountNumber;
    private String failureReason;
    private BigDecimal amount;
    private String correlationId;
    private LocalDateTime occurredAt;
}
