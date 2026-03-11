package com.banking.beneficiary.service;

import com.banking.beneficiary.dto.BeneficiaryResponse;
import com.banking.beneficiary.dto.CreateBeneficiaryRequest;
import com.banking.beneficiary.entity.Beneficiary;
import com.banking.beneficiary.exception.BeneficiaryException;
import com.banking.beneficiary.mapper.BeneficiaryMapper;
import com.banking.beneficiary.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final BeneficiaryMapper beneficiaryMapper;

    @Transactional
    public BeneficiaryResponse addBeneficiary(UUID userId, CreateBeneficiaryRequest request) {
        if (beneficiaryRepository.existsByUserIdAndAccountNumber(userId, request.getAccountNumber())) {
            throw new BeneficiaryException("Beneficiary with account number " +
                    request.getAccountNumber() + " already exists");
        }

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setUserId(userId);
        beneficiary.setNickname(request.getNickname());
        beneficiary.setAccountNumber(request.getAccountNumber());
        beneficiary.setAccountHolderName(request.getAccountHolderName());
        beneficiary.setBankName(request.getBankName());
        beneficiary.setBankCode(request.getBankCode());
        beneficiary.setCurrency(request.getCurrency());
        beneficiary.setActive(true);
        beneficiary = beneficiaryRepository.save(beneficiary);

        log.info("Beneficiary [{}] added for user [{}]", beneficiary.getId(), userId);
        return beneficiaryMapper.toResponse(beneficiary);
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiaries(UUID userId) {
        return beneficiaryRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(beneficiaryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryById(UUID beneficiaryId, UUID userId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new BeneficiaryException("Beneficiary not found: " + beneficiaryId));
        if (!beneficiary.getUserId().equals(userId)) {
            throw new BeneficiaryException("Beneficiary not found: " + beneficiaryId);
        }
        return beneficiaryMapper.toResponse(beneficiary);
    }

    @Transactional
    public void deleteBeneficiary(UUID beneficiaryId, UUID userId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new BeneficiaryException("Beneficiary not found: " + beneficiaryId));
        if (!beneficiary.getUserId().equals(userId)) {
            throw new BeneficiaryException("Beneficiary not found: " + beneficiaryId);
        }
        beneficiary.setActive(false);
        beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary [{}] deactivated for user [{}]", beneficiaryId, userId);
    }
}
