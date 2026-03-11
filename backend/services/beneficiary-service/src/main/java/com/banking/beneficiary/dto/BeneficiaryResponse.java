package com.banking.beneficiary.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BeneficiaryResponse {
    private UUID id;
    private String nickname;
    private String accountNumber;
    private String accountHolderName;
    private String bankName;
    private String bankCode;
    private String currency;
    private boolean active;
    private LocalDateTime createdAt;
}
