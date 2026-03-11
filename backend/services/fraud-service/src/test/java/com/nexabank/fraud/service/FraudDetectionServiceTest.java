package com.banking.fraud.service;

import com.banking.fraud.entity.FraudAlert;
import com.banking.fraud.entity.FraudAlertStatus;
import com.banking.fraud.entity.FraudRuleType;
import com.banking.fraud.event.TransactionCreatedEvent;
import com.banking.fraud.repository.FraudAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock FraudAlertRepository fraudAlertRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks FraudDetectionService fraudDetectionService;

    private TransactionCreatedEvent event;
    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fraudDetectionService, "largeTransactionThreshold", new BigDecimal("10000"));
        ReflectionTestUtils.setField(fraudDetectionService, "rapidTransferWindowMinutes", 5);
        ReflectionTestUtils.setField(fraudDetectionService, "rapidTransferMaxCount", 3);

        event = new TransactionCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTransactionReference("TXN-001");
        event.setUserId(USER_ID);
        event.setSourceAccountNumber("ACC0000000000001");
        event.setDestinationAccountNumber("ACC0000000000002");
        event.setTransactionType("TRANSFER");
        event.setAmount(new BigDecimal("500.00"));
        event.setCurrency("USD");
        event.setOccurredAt(LocalDateTime.now());
    }

    @Test
    void analyze_SmallTransaction_NoAlertCreated() {
        when(fraudAlertRepository.countRecentAlertsByUser(any(UUID.class), any())).thenReturn(0L);

        fraudDetectionService.analyzeTransaction(event);

        verify(fraudAlertRepository, never()).save(any());
    }

    @Test
    void analyze_LargeTransaction_CreatesLargeTransactionAlert() {
        event.setAmount(new BigDecimal("15000.00"));

        when(fraudAlertRepository.save(any())).thenAnswer(inv -> {
            FraudAlert a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });

        fraudDetectionService.analyzeTransaction(event);

        ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository, atLeastOnce()).save(captor.capture());

        boolean hasLargeTransactionRule = captor.getAllValues().stream()
            .anyMatch(a -> a.getRuleTriggered() == FraudRuleType.LARGE_TRANSACTION);
        assertThat(hasLargeTransactionRule).isTrue();
    }

    @Test
    void analyze_LargeTransaction_PublishesToFraudAlertsTopic() {
        event.setAmount(new BigDecimal("25000.00"));

        when(fraudAlertRepository.save(any())).thenAnswer(inv -> {
            FraudAlert a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });

        fraudDetectionService.analyzeTransaction(event);

        verify(kafkaTemplate, atLeastOnce()).send(eq("fraud-alerts"), any(), any());
    }

    @Test
    void analyze_RapidTransfers_CreatesRapidTransferAlert() {
        event.setAmount(new BigDecimal("100.00"));
        when(fraudAlertRepository.countRecentAlertsByUser(any(UUID.class), any())).thenReturn(4L);
        when(fraudAlertRepository.save(any())).thenAnswer(inv -> {
            FraudAlert a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });

        fraudDetectionService.analyzeTransaction(event);

        ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository, atLeastOnce()).save(captor.capture());

        boolean hasRapidRule = captor.getAllValues().stream()
            .anyMatch(a -> a.getRuleTriggered() == FraudRuleType.RAPID_REPEATED_TRANSFERS);
        assertThat(hasRapidRule).isTrue();
    }

    @Test
    void analyze_AlertCreated_HasOpenStatus() {
        event.setAmount(new BigDecimal("50000.00"));

        when(fraudAlertRepository.save(any())).thenAnswer(inv -> {
            FraudAlert a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });

        fraudDetectionService.analyzeTransaction(event);

        ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository, atLeastOnce()).save(captor.capture());

        captor.getAllValues().forEach(a ->
            assertThat(a.getStatus()).isEqualTo(FraudAlertStatus.OPEN));
    }
}
