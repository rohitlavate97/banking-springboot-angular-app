package com.banking.fraud.dto;

import com.banking.fraud.entity.FraudAlertStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateFraudAlertRequest {
    @NotNull(message = "Status is required")
    private FraudAlertStatus status;
}
