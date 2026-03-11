package com.banking.transaction.service;

import com.banking.transaction.client.AccountServiceClient;
import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionStatus;
import com.banking.transaction.entity.TransactionType;
import com.banking.transaction.event.TransactionCompletedEvent;
import com.banking.transaction.event.TransactionCreatedEvent;
import com.banking.transaction.event.TransactionEventPublisher;
import com.banking.transaction.event.TransactionFailedEvent;
import com.banking.transaction.exception.TransactionException;
import com.banking.transaction.mapper.TransactionMapper;
import com.banking.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventPublisher eventPublisher;
    private final AccountServiceClient accountServiceClient;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse deposit(TransactionRequest request, UUID userId, String correlationId) {
        log.info("Processing deposit for user [{}] correlationId [{}]", userId, correlationId);

        Transaction transaction = buildTransaction(request, userId, TransactionType.DEPOSIT, correlationId);
        transaction.setSourceAccountNumber(null);
        transaction = transactionRepository.save(transaction);

        publishCreatedEvent(transaction);

        try {
            transaction = updateStatus(transaction, TransactionStatus.PROCESSING);
            accountServiceClient.credit(request.getDestinationAccountNumber(), request.getAmount(), correlationId);
            transaction = updateStatus(transaction, TransactionStatus.COMPLETED);
            publishCompletedEvent(transaction);
            log.info("Deposit completed for transaction [{}]", transaction.getReferenceNumber());
        } catch (Exception ex) {
            log.error("Deposit failed for transaction [{}]: {}", transaction.getReferenceNumber(), ex.getMessage(), ex);
            transaction = failTransaction(transaction, ex.getMessage());
            publishFailedEvent(transaction);
            throw new TransactionException("Deposit failed: " + ex.getMessage(), ex);
        }

        return transactionMapper.toResponse(transaction);
    }

    @Transactional
    public TransactionResponse withdraw(TransactionRequest request, UUID userId, String correlationId) {
        log.info("Processing withdrawal for user [{}] correlationId [{}]", userId, correlationId);

        Transaction transaction = buildTransaction(request, userId, TransactionType.WITHDRAWAL, correlationId);
        transaction.setDestinationAccountNumber(null);
        transaction = transactionRepository.save(transaction);

        publishCreatedEvent(transaction);

        try {
            transaction = updateStatus(transaction, TransactionStatus.PROCESSING);
            accountServiceClient.debit(request.getSourceAccountNumber(), request.getAmount(), correlationId);
            transaction = updateStatus(transaction, TransactionStatus.COMPLETED);
            publishCompletedEvent(transaction);
            log.info("Withdrawal completed for transaction [{}]", transaction.getReferenceNumber());
        } catch (Exception ex) {
            log.error("Withdrawal failed for transaction [{}]: {}", transaction.getReferenceNumber(), ex.getMessage(), ex);
            transaction = failTransaction(transaction, ex.getMessage());
            publishFailedEvent(transaction);
            throw new TransactionException("Withdrawal failed: " + ex.getMessage(), ex);
        }

        return transactionMapper.toResponse(transaction);
    }

    @Transactional
    @CircuitBreaker(name = "account-service", fallbackMethod = "transferFallback")
    @Retry(name = "account-service")
    public TransactionResponse transfer(TransactionRequest request, UUID userId, String correlationId) {
        log.info("Processing transfer for user [{}] correlationId [{}]", userId, correlationId);

        Transaction transaction = buildTransaction(request, userId, TransactionType.TRANSFER, correlationId);
        transaction = transactionRepository.save(transaction);

        publishCreatedEvent(transaction);

        try {
            transaction = updateStatus(transaction, TransactionStatus.PROCESSING);

            // Saga Step 1: Debit source account
            accountServiceClient.debit(request.getSourceAccountNumber(), request.getAmount(), correlationId);
            log.debug("Debited source account [{}] for transaction [{}]",
                    request.getSourceAccountNumber(), transaction.getReferenceNumber());

            try {
                // Saga Step 2: Credit destination account
                accountServiceClient.credit(request.getDestinationAccountNumber(), request.getAmount(), correlationId);
                log.debug("Credited destination account [{}] for transaction [{}]",
                        request.getDestinationAccountNumber(), transaction.getReferenceNumber());

                transaction = updateStatus(transaction, TransactionStatus.COMPLETED);
                publishCompletedEvent(transaction);
                log.info("Transfer completed for transaction [{}]", transaction.getReferenceNumber());

            } catch (Exception creditEx) {
                log.error("Credit failed for transaction [{}], initiating compensating debit: {}",
                        transaction.getReferenceNumber(), creditEx.getMessage());

                // Compensating transaction: re-credit the source account
                try {
                    accountServiceClient.credit(request.getSourceAccountNumber(), request.getAmount(), correlationId);
                    log.info("Compensation successful for transaction [{}]", transaction.getReferenceNumber());
                } catch (Exception compensateEx) {
                    log.error("CRITICAL: Compensation failed for transaction [{}]: {}",
                            transaction.getReferenceNumber(), compensateEx.getMessage(), compensateEx);
                }

                transaction = rolledBackTransaction(transaction, creditEx.getMessage());
                publishFailedEvent(transaction);
                throw new TransactionException("Transfer failed during credit, compensation applied: " + creditEx.getMessage(), creditEx);
            }

        } catch (TransactionException te) {
            throw te;
        } catch (Exception ex) {
            log.error("Transfer debit failed for transaction [{}]: {}", transaction.getReferenceNumber(), ex.getMessage(), ex);
            transaction = failTransaction(transaction, ex.getMessage());
            publishFailedEvent(transaction);
            throw new TransactionException("Transfer debit failed: " + ex.getMessage(), ex);
        }

        return transactionMapper.toResponse(transaction);
    }

    public TransactionResponse transferFallback(TransactionRequest request, UUID userId, String correlationId, Throwable ex) {
        log.error("Transfer circuit breaker fallback triggered for user [{}]: {}", userId, ex.getMessage());
        throw new TransactionException("Transfer service temporarily unavailable. Please try again later.", ex);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByUser(UUID userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable)
                .map(transactionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID id, UUID userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionException("Transaction not found: " + id));
        if (!transaction.getUserId().equals(userId)) {
            throw new TransactionException("Transaction not found: " + id);
        }
        return transactionMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String referenceNumber, UUID userId) {
        Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new TransactionException("Transaction not found: " + referenceNumber));
        if (!transaction.getUserId().equals(userId)) {
            throw new TransactionException("Transaction not found: " + referenceNumber);
        }
        return transactionMapper.toResponse(transaction);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Transaction buildTransaction(TransactionRequest request, UUID userId,
                                         TransactionType type, String correlationId) {
        Transaction t = new Transaction();
        t.setUserId(userId);
        t.setSourceAccountNumber(request.getSourceAccountNumber());
        t.setDestinationAccountNumber(request.getDestinationAccountNumber());
        t.setType(type);
        t.setStatus(TransactionStatus.PENDING);
        t.setAmount(request.getAmount());
        t.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        t.setDescription(request.getDescription());
        t.setCorrelationId(correlationId);
        return t;
    }

    private Transaction updateStatus(Transaction transaction, TransactionStatus status) {
        transaction.setStatus(status);
        return transactionRepository.save(transaction);
    }

    private Transaction failTransaction(Transaction transaction, String reason) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(reason);
        return transactionRepository.save(transaction);
    }

    private Transaction rolledBackTransaction(Transaction transaction, String reason) {
        transaction.setStatus(TransactionStatus.ROLLED_BACK);
        transaction.setFailureReason(reason);
        return transactionRepository.save(transaction);
    }

    private void publishCreatedEvent(Transaction t) {
        eventPublisher.publishTransactionCreated(TransactionCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionReference(t.getReferenceNumber())
                .userId(t.getUserId().toString())
                .sourceAccountNumber(t.getSourceAccountNumber())
                .destinationAccountNumber(t.getDestinationAccountNumber())
                .transactionType(t.getType().name())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .correlationId(t.getCorrelationId())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private void publishCompletedEvent(Transaction t) {
        eventPublisher.publishTransactionCompleted(TransactionCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionReference(t.getReferenceNumber())
                .userId(t.getUserId().toString())
                .sourceAccountNumber(t.getSourceAccountNumber())
                .destinationAccountNumber(t.getDestinationAccountNumber())
                .transactionType(t.getType().name())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .correlationId(t.getCorrelationId())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private void publishFailedEvent(Transaction t) {
        eventPublisher.publishTransactionFailed(TransactionFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionReference(t.getReferenceNumber())
                .userId(t.getUserId().toString())
                .sourceAccountNumber(t.getSourceAccountNumber())
                .destinationAccountNumber(t.getDestinationAccountNumber())
                .transactionType(t.getType().name())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .failureReason(t.getFailureReason())
                .correlationId(t.getCorrelationId())
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
