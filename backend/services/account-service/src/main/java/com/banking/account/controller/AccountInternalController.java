package com.banking.account.controller;

import com.banking.account.exception.AccountException;
import com.banking.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/accounts/internal")
@RequiredArgsConstructor
public class AccountInternalController {

    private final AccountService accountService;

    @PostMapping("/debit")
    public ResponseEntity<Void> debit(@RequestBody Map<String, Object> body,
                                      @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String accountNumber = (String) body.get("accountNumber");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        log.info("Internal debit request for account [{}] amount [{}] correlationId [{}]",
                accountNumber, amount, correlationId);
        accountService.debitByAccountNumber(accountNumber, amount);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/credit")
    public ResponseEntity<Void> credit(@RequestBody Map<String, Object> body,
                                       @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String accountNumber = (String) body.get("accountNumber");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        log.info("Internal credit request for account [{}] amount [{}] correlationId [{}]",
                accountNumber, amount, correlationId);
        accountService.creditByAccountNumber(accountNumber, amount);
        return ResponseEntity.ok().build();
    }
}
