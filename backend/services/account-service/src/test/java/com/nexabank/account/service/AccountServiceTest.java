package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.entity.Account;
import com.banking.account.entity.AccountStatus;
import com.banking.account.entity.AccountType;
import com.banking.account.exception.AccountException;
import com.banking.account.mapper.AccountMapper;
import com.banking.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock AccountMapper accountMapper;

    @InjectMocks AccountService accountService;

    private Account account;
    private static final String ACCOUNT_NUMBER = "ACC0000000000001";

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(UUID.randomUUID());
        account.setUserId(UUID.randomUUID());
        account.setAccountNumber(ACCOUNT_NUMBER);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAvailableBalance(new BigDecimal("1000.00"));
        account.setAccountType(AccountType.SAVINGS);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCurrency("USD");
    }

    @Test
    void debit_SufficientFunds_DeductsBalance() {
        when(accountRepository.findByAccountNumberWithLock(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(accountMapper.toResponse(any())).thenReturn(new AccountResponse());

        accountService.debit(ACCOUNT_NUMBER, new BigDecimal("200.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("800.00");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("800.00");
        verify(accountRepository).save(account);
    }

    @Test
    void debit_InsufficientFunds_ThrowsAccountException() {
        when(accountRepository.findByAccountNumberWithLock(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.debit(ACCOUNT_NUMBER, new BigDecimal("5000.00")))
            .isInstanceOf(AccountException.class)
            .hasMessageContaining("Insufficient");
    }

    @Test
    void debit_FrozenAccount_ThrowsAccountException() {
        account.setStatus(AccountStatus.SUSPENDED);
        when(accountRepository.findByAccountNumberWithLock(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.debit(ACCOUNT_NUMBER, new BigDecimal("100.00")))
            .isInstanceOf(AccountException.class);
    }

    @Test
    void credit_AddsToBalance() {
        when(accountRepository.findByAccountNumberWithLock(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(accountMapper.toResponse(any())).thenReturn(new AccountResponse());

        accountService.credit(ACCOUNT_NUMBER, new BigDecimal("500.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("1500.00");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("1500.00");
    }

    @Test
    void debit_AccountNotFound_ThrowsAccountException() {
        when(accountRepository.findByAccountNumberWithLock("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.debit("NONEXISTENT", new BigDecimal("10.00")))
            .isInstanceOf(AccountException.class);
    }
}
