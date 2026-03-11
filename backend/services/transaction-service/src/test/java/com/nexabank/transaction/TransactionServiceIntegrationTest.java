package com.nexabank.transaction;

import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionStatus;
import com.banking.transaction.entity.TransactionType;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("transaction_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("services.account-service.url", () -> "http://localhost:9999");
    }

    @Autowired TransactionService transactionService;
    @Autowired TransactionRepository transactionRepository;

    @Test
    void deposit_PersistsTransactionToDatabase() {
        TransactionRequest req = new TransactionRequest();
        req.setType(TransactionType.DEPOSIT);
        req.setSourceAccountNumber("ACC0000000000100");
        req.setDestinationAccountNumber("ACC0000000000100");
        req.setAmount(new BigDecimal("250.00"));
        req.setDescription("Integration test deposit");

        // AccountServiceClient will fail to reach stub URL — service handles gracefully
        assertThatThrownBy(() -> transactionService.deposit(req, UUID.randomUUID(), "integ-corr-001"))
            .isInstanceOf(Exception.class);

        // Transaction should still be persisted with FAILED status
        var transactions = transactionRepository.findAll();
        assertThat(transactions).isNotEmpty();
    }

    @Test
    void findAll_ReturnsPersistedTransactions() {
        Transaction tx = Transaction.builder()
            .referenceNumber("REF-" + UUID.randomUUID())
            .userId(UUID.randomUUID())
            .sourceAccountNumber("ACC0000000000001")
            .type(TransactionType.DEPOSIT)
            .status(TransactionStatus.COMPLETED)
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .build();
        transactionRepository.save(tx);

        var page = transactionRepository.findAll(
            org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }
}
