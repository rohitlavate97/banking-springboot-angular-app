package com.nexabank.transaction.service;

import com.nexabank.transaction.client.AccountServiceClient;
import com.nexabank.transaction.dto.TransactionRequest;
import com.nexabank.transaction.entity.Transaction;
import com.nexabank.transaction.entity.TransactionStatus;
import com.nexabank.transaction.entity.TransactionType;
import com.nexabank.transaction.exception.TransactionException;
import com.nexabank.transaction.kafka.TransactionEventPublisher;
import com.nexabank.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock AccountServiceClient accountServiceClient;
    @Mock TransactionEventPublisher eventPublisher;

    @InjectMocks TransactionService transactionService;

    @BeforeEach
    void setUp() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) t.setId(1L);
            return t;
        });
    }

    // ---- Transfer (Saga happy path) ----

    @Test
    void transfer_Success_EmitsCompletedEvent() {
        TransactionRequest req = new TransactionRequest();
        req.setSourceAccountId(1L);
        req.setDestinationAccountId(2L);
        req.setAmount(new BigDecimal("300.00"));
        req.setDescription("test transfer");

        doNothing().when(accountServiceClient).debit(eq(1L), any());
        doNothing().when(accountServiceClient).credit(eq(2L), any());
        doNothing().when(eventPublisher).publishTransactionCreated(any());
        doNothing().when(eventPublisher).publishTransactionCompleted(any());

        Transaction result = transactionService.transfer(req, 10L);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(eventPublisher).publishTransactionCompleted(any());
        verify(eventPublisher, never()).publishTransactionFailed(any());
    }

    @Test
    void transfer_CreditFails_CompensatesAndEmitsFailedEvent() {
        TransactionRequest req = new TransactionRequest();
        req.setSourceAccountId(1L);
        req.setDestinationAccountId(2L);
        req.setAmount(new BigDecimal("300.00"));

        doNothing().when(accountServiceClient).debit(eq(1L), any());
        doThrow(new RuntimeException("Credit failed")).when(accountServiceClient).credit(eq(2L), any());
        doNothing().when(accountServiceClient).credit(eq(1L), any()); // compensation
        doNothing().when(eventPublisher).publishTransactionCreated(any());
        doNothing().when(eventPublisher).publishTransactionFailed(any());

        Transaction result = transactionService.transfer(req, 10L);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.ROLLED_BACK);
        verify(accountServiceClient).credit(eq(1L), any()); // compensating credit
        verify(eventPublisher).publishTransactionFailed(any());
    }

    @Test
    void transfer_DebitFails_SetsFailedStatus() {
        TransactionRequest req = new TransactionRequest();
        req.setSourceAccountId(1L);
        req.setDestinationAccountId(2L);
        req.setAmount(new BigDecimal("300.00"));

        doThrow(new RuntimeException("Insufficient funds")).when(accountServiceClient).debit(eq(1L), any());
        doNothing().when(eventPublisher).publishTransactionCreated(any());
        doNothing().when(eventPublisher).publishTransactionFailed(any());

        Transaction result = transactionService.transfer(req, 10L);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED);
        verify(accountServiceClient, never()).credit(any(), any());
    }

    // ---- Deposit ----

    @Test
    void deposit_Success_ReturnsCompleted() {
        TransactionRequest req = new TransactionRequest();
        req.setDestinationAccountId(2L);
        req.setAmount(new BigDecimal("100.00"));

        doNothing().when(accountServiceClient).credit(eq(2L), any());
        doNothing().when(eventPublisher).publishTransactionCompleted(any());

        Transaction result = transactionService.deposit(req, 10L);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
    }

    // ---- Withdraw ----

    @Test
    void withdraw_Success_ReturnsCompleted() {
        TransactionRequest req = new TransactionRequest();
        req.setSourceAccountId(1L);
        req.setAmount(new BigDecimal("50.00"));

        doNothing().when(accountServiceClient).debit(eq(1L), any());
        doNothing().when(eventPublisher).publishTransactionCompleted(any());

        Transaction result = transactionService.withdraw(req, 10L);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    @Test
    void transfer_NullAmount_ThrowsTransactionException() {
        TransactionRequest req = new TransactionRequest();
        req.setSourceAccountId(1L);
        req.setDestinationAccountId(2L);
        req.setAmount(null);

        assertThatThrownBy(() -> transactionService.transfer(req, 10L))
            .isInstanceOf(TransactionException.class);
    }
}
