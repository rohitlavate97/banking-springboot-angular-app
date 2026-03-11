package com.banking.transaction.dto;

import com.banking.transaction.entity.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotBlank(message = "Source account number is required")
    private String sourceAccountNumber;

    private String destinationAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;

    @Pattern(regexp = "^[A-Z]{3}$")
    private String currency = "USD";

    @Size(max = 255)
    private String description;
}
