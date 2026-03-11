package com.nexabank.transaction;

import com.nexabank.transaction.dto.TransactionRequest;
import com.nexabank.transaction.entity.Transaction;
import com.nexabank.transaction.entity.TransactionStatus;
import com.nexabank.transaction.entity.TransactionType;
import com.nexabank.transaction.repository.TransactionRepository;
import com.nexabank.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("transaction_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        // stub account-service calls
        registry.add("app.account-service.url", () -> "http://localhost:9999");
    }

    @Autowired TransactionService transactionService;
    @Autowired TransactionRepository transactionRepository;

    @Test
    void deposit_PersistsTransactionToDatabase() {
        TransactionRequest req = new TransactionRequest();
        req.setDestinationAccountId(100L);
        req.setAmount(new BigDecimal("250.00"));
        req.setDescription("Integration test deposit");

        // AccountServiceClient will fail to reach stub URL — service handles gracefully
        Transaction tx = transactionService.deposit(req, 1L);

        assertThat(tx.getId()).isNotNull();
        assertThat(transactionRepository.findById(tx.getId())).isPresent();
        assertThat(tx.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(tx.getAmount()).isEqualByComparingTo("250.00");
    }

    @Test
    void findAll_ReturnsPersistedTransactions() {
        // Create a record directly
        Transaction tx = new Transaction();
        tx.setUserId(1L);
        tx.setAmount(new BigDecimal("100.00"));
        tx.setTransactionType(TransactionType.DEPOSIT);
        tx.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(tx);

        var page = transactionRepository.findByUserId(1L,
            org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }
}
