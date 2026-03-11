package com.banking.fraud.controller;

import com.banking.fraud.dto.FraudAlertResponse;
import com.banking.fraud.dto.UpdateFraudAlertRequest;
import com.banking.fraud.service.FraudAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;

    @GetMapping("/alerts")
    public ResponseEntity<Page<FraudAlertResponse>> getAllAlerts(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(fraudAlertService.getAllAlerts(pageable));
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<FraudAlertResponse> getAlertById(@PathVariable UUID id) {
        return ResponseEntity.ok(fraudAlertService.getAlertById(id));
    }

    @PatchMapping("/alerts/{id}/status")
    public ResponseEntity<FraudAlertResponse> updateStatus(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateFraudAlertRequest request) {
        return ResponseEntity.ok(fraudAlertService.updateAlertStatus(id, request.getStatus()));
    }
}
