package com.banking.beneficiary.controller;

import com.banking.beneficiary.dto.BeneficiaryResponse;
import com.banking.beneficiary.dto.CreateBeneficiaryRequest;
import com.banking.beneficiary.service.BeneficiaryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> add(@Valid @RequestBody CreateBeneficiaryRequest request,
                                                    HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(beneficiaryService.addBeneficiary(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> getAll(HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        return ResponseEntity.ok(beneficiaryService.getBeneficiaries(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BeneficiaryResponse> getById(@PathVariable UUID id, HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        return ResponseEntity.ok(beneficiaryService.getBeneficiaryById(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        beneficiaryService.deleteBeneficiary(id, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(HttpServletRequest request) {
        String userHeader = request.getHeader("X-Authenticated-User");
        if (userHeader == null) {
            throw new IllegalStateException("Missing X-Authenticated-User header");
        }
        return UUID.fromString(userHeader);
    }
}
