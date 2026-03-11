package com.banking.transaction.controller;

import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody TransactionRequest request,
                                                       HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        String correlationId = getCorrelationId(httpRequest);
        return ResponseEntity.ok(transactionService.deposit(request, userId, correlationId));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody TransactionRequest request,
                                                        HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        String correlationId = getCorrelationId(httpRequest);
        return ResponseEntity.ok(transactionService.withdraw(request, userId, correlationId));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransactionRequest request,
                                                        HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        String correlationId = getCorrelationId(httpRequest);
        return ResponseEntity.ok(transactionService.transfer(request, userId, correlationId));
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        return ResponseEntity.ok(transactionService.getTransactionsByUser(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable UUID id, HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        return ResponseEntity.ok(transactionService.getTransactionById(id, userId));
    }

    @GetMapping("/reference/{referenceNumber}")
    public ResponseEntity<TransactionResponse> getByReference(@PathVariable String referenceNumber,
                                                              HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        return ResponseEntity.ok(transactionService.getTransactionByReference(referenceNumber, userId));
    }

    private UUID extractUserId(HttpServletRequest request) {
        String userHeader = request.getHeader("X-Authenticated-User");
        if (userHeader == null) {
            throw new IllegalStateException("Missing X-Authenticated-User header");
        }
        return UUID.fromString(userHeader);
    }

    private String getCorrelationId(HttpServletRequest request) {
        String id = request.getHeader("X-Correlation-ID");
        return id != null ? id : UUID.randomUUID().toString();
    }
}
