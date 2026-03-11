package com.banking.account.dto;

import com.banking.account.entity.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAccountRequest {
    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency = "USD";

    @Size(max = 100)
    private String alias;
}
