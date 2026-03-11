package com.banking.transaction.dto;

import com.banking.transaction.entity.TransactionStatus;
import com.banking.transaction.entity.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private String referenceNumber;
    private UUID userId;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
}
