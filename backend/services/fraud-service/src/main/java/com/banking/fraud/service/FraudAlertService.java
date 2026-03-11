package com.banking.fraud.service;

import com.banking.fraud.dto.FraudAlertResponse;
import com.banking.fraud.entity.FraudAlert;
import com.banking.fraud.entity.FraudAlertStatus;
import com.banking.fraud.mapper.FraudAlertMapper;
import com.banking.fraud.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;
    private final FraudAlertMapper fraudAlertMapper;

    @Transactional(readOnly = true)
    public Page<FraudAlertResponse> getAllAlerts(Pageable pageable) {
        return fraudAlertRepository.findAll(pageable).map(fraudAlertMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public FraudAlertResponse getAlertById(UUID id) {
        return fraudAlertRepository.findById(id)
                .map(fraudAlertMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Fraud alert not found: " + id));
    }

    @Transactional
    public FraudAlertResponse updateAlertStatus(UUID id, FraudAlertStatus newStatus) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fraud alert not found: " + id));
        alert.setStatus(newStatus);
        alert = fraudAlertRepository.save(alert);
        log.info("Fraud alert [{}] status updated to [{}]", id, newStatus);
        return fraudAlertMapper.toResponse(alert);
    }
}
