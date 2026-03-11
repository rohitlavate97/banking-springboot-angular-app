package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.entity.Account;
import com.banking.account.entity.AccountStatus;
import com.banking.account.exception.AccountException;
import com.banking.account.mapper.AccountMapper;
import com.banking.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse createAccount(UUID userId, CreateAccountRequest request) {
        String accountNumber = generateUniqueAccountNumber();
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .userId(userId)
                .accountType(request.getAccountType())
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .alias(request.getAlias())
                .build();
        Account saved = accountRepository.save(account);
        log.info("Account created: {} for userId: {}", saved.getAccountNumber(), userId);
        return accountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(UUID userId) {
        return accountRepository.findByUserId(userId)
                .stream().map(accountMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "accounts", key = "#accountId")
    public AccountResponse getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException("Account not found: " + accountId));
        return accountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "accounts-by-number", key = "#accountNumber")
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException("Account not found: " + accountNumber));
        return accountMapper.toResponse(account);
    }

    @Transactional
    @CacheEvict(value = {"accounts", "accounts-by-number"}, allEntries = true)
    public AccountResponse debit(String accountNumber, BigDecimal amount) {
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountException("Account not found: " + accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountException("Account is not active: " + accountNumber);
        }
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new AccountException("Insufficient funds in account: " + accountNumber);
        }

        account.setBalance(account.getBalance().subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        Account updated = accountRepository.save(account);
        log.info("Debited {} from account: {}", amount, accountNumber);
        return accountMapper.toResponse(updated);
    }

    @Transactional
    @CacheEvict(value = {"accounts", "accounts-by-number"}, allEntries = true)
    public AccountResponse credit(String accountNumber, BigDecimal amount) {
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountException("Account not found: " + accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountException("Account is not active: " + accountNumber);
        }

        account.setBalance(account.getBalance().add(amount));
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        Account updated = accountRepository.save(account);
        log.info("Credited {} to account: {}", amount, accountNumber);
        return accountMapper.toResponse(updated);
    }

    public AccountResponse debitByAccountNumber(String accountNumber, BigDecimal amount) {
        return debit(accountNumber, amount);
    }

    public AccountResponse creditByAccountNumber(String accountNumber, BigDecimal amount) {
        return credit(accountNumber, amount);
    }

    private String generateUniqueAccountNumber() {
        String number;
        do {
            number = "ACC" + String.format("%013d", (long) (new Random().nextDouble() * 1_000_000_000_000_0L));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
