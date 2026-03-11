package com.banking.beneficiary.mapper;

import com.banking.beneficiary.dto.BeneficiaryResponse;
import com.banking.beneficiary.entity.Beneficiary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BeneficiaryMapper {
    BeneficiaryResponse toResponse(Beneficiary beneficiary);
}
