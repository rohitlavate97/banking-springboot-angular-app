package com.nexabank.account.service;

import com.nexabank.account.entity.Account;
import com.nexabank.account.entity.AccountStatus;
import com.nexabank.account.entity.AccountType;
import com.nexabank.account.exception.AccountException;
import com.nexabank.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOps;

    @InjectMocks AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setUserId(10L);
        account.setAccountNumber("ACC-001");
        account.setBalance(new BigDecimal("1000.00"));
        account.setAccountType(AccountType.SAVINGS);
        account.setStatus(AccountStatus.ACTIVE);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void debit_SufficientFunds_DeductsBalance() {
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);

        accountService.debit(1L, new BigDecimal("200.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("800.00");
        verify(accountRepository).save(account);
    }

    @Test
    void debit_InsufficientFunds_ThrowsAccountException() {
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.debit(1L, new BigDecimal("5000.00")))
            .isInstanceOf(AccountException.class)
            .hasMessageContaining("Insufficient");
    }

    @Test
    void debit_FrozenAccount_ThrowsAccountException() {
        account.setStatus(AccountStatus.FROZEN);
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.debit(1L, new BigDecimal("100.00")))
            .isInstanceOf(AccountException.class);
    }

    @Test
    void credit_AddsToBalance() {
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);

        accountService.credit(1L, new BigDecimal("500.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("1500.00");
    }

    @Test
    void debit_AccountNotFound_ThrowsAccountException() {
        when(accountRepository.findByIdWithLock(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.debit(99L, new BigDecimal("10.00")))
            .isInstanceOf(AccountException.class);
    }

    @Test
    void debit_ZeroAmount_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> accountService.debit(1L, BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void credit_NegativeAmount_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> accountService.credit(1L, new BigDecimal("-50.00")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
