package com.banking.fraud.mapper;

import com.banking.fraud.dto.FraudAlertResponse;
import com.banking.fraud.entity.FraudAlert;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FraudAlertMapper {
    FraudAlertResponse toResponse(FraudAlert alert);
}
