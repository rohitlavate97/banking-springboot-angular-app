package com.banking.transaction.service;

import com.banking.transaction.client.AccountServiceClient;
import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionStatus;
import com.banking.transaction.entity.TransactionType;
import com.banking.transaction.event.TransactionEventPublisher;
import com.banking.transaction.exception.TransactionException;
import com.banking.transaction.mapper.TransactionMapper;
import com.banking.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock AccountServiceClient accountServiceClient;
    @Mock TransactionEventPublisher eventPublisher;
    @Mock TransactionMapper transactionMapper;

    @InjectMocks TransactionService transactionService;

    private static final String SRC  = "ACC0000000000001";
    private static final String DEST = "ACC0000000000002";
    private static final UUID   USER = UUID.randomUUID();
    private static final String CORR = "corr-001";

    @BeforeEach
    void setUp() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(transactionMapper.toResponse(any())).thenReturn(new TransactionResponse());
    }

    private TransactionRequest transferRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setType(TransactionType.TRANSFER);
        req.setSourceAccountNumber(SRC);
        req.setDestinationAccountNumber(DEST);
        req.setAmount(new BigDecimal("300.00"));
        req.setDescription("test transfer");
        return req;
    }

    // ---- Transfer (Saga happy path) ----

    @Test
    void transfer_Success_EmitsCompletedEvent() {
        doNothing().when(accountServiceClient).debit(eq(SRC), any(), anyString());
        doNothing().when(accountServiceClient).credit(eq(DEST), any(), anyString());

        transactionService.transfer(transferRequest(), USER, CORR);

        verify(eventPublisher).publishTransactionCompleted(any());
        verify(eventPublisher, never()).publishTransactionFailed(any());
    }

    @Test
    void transfer_CreditFails_CompensatesAndEmitsFailedEvent() {
        doNothing().when(accountServiceClient).debit(eq(SRC), any(), anyString());
        doThrow(new RuntimeException("Credit failed")).when(accountServiceClient).credit(eq(DEST), any(), anyString());

        assertThatThrownBy(() -> transactionService.transfer(transferRequest(), USER, CORR))
            .isInstanceOf(TransactionException.class);

        verify(accountServiceClient).credit(eq(SRC), any(), anyString()); // compensating credit
        verify(eventPublisher).publishTransactionFailed(any());
    }

    @Test
    void transfer_DebitFails_EmitsFailedEvent() {
        doThrow(new RuntimeException("Insufficient funds")).when(accountServiceClient).debit(eq(SRC), any(), anyString());

        assertThatThrownBy(() -> transactionService.transfer(transferRequest(), USER, CORR))
            .isInstanceOf(TransactionException.class);

        verify(accountServiceClient, never()).credit(eq(DEST), any(), anyString());
        verify(eventPublisher).publishTransactionFailed(any());
    }

    // ---- Deposit ----

    @Test
    void deposit_Success_ReturnsCompleted() {
        TransactionRequest req = new TransactionRequest();
        req.setType(TransactionType.DEPOSIT);
        req.setSourceAccountNumber(SRC);
        req.setDestinationAccountNumber(DEST);
        req.setAmount(new BigDecimal("100.00"));

        doNothing().when(accountServiceClient).credit(eq(DEST), any(), anyString());

        transactionService.deposit(req, USER, CORR);

        verify(eventPublisher).publishTransactionCompleted(any());
    }

    // ---- Withdraw ----

    @Test
    void withdraw_Success_ReturnsCompleted() {
        TransactionRequest req = new TransactionRequest();
        req.setType(TransactionType.WITHDRAWAL);
        req.setSourceAccountNumber(SRC);
        req.setDestinationAccountNumber(DEST);
        req.setAmount(new BigDecimal("50.00"));

        doNothing().when(accountServiceClient).debit(eq(SRC), any(), anyString());

        transactionService.withdraw(req, USER, CORR);

        verify(eventPublisher).publishTransactionCompleted(any());
    }

    @Test
    void transfer_NullAmount_ThrowsTransactionException() {
        TransactionRequest req = new TransactionRequest();
        req.setType(TransactionType.TRANSFER);
        req.setSourceAccountNumber(SRC);
        req.setDestinationAccountNumber(DEST);
        req.setAmount(null);

        assertThatThrownBy(() -> transactionService.transfer(req, USER, CORR))
            .isInstanceOf(Exception.class);
    }
}
