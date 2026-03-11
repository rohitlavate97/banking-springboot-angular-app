package com.banking.account.dto;

import com.banking.account.entity.AccountStatus;
import com.banking.account.entity.AccountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private UUID id;
    private String accountNumber;
    private UUID userId;
    private AccountType accountType;
    private AccountStatus status;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private String currency;
    private String alias;
    private LocalDateTime createdAt;
}
