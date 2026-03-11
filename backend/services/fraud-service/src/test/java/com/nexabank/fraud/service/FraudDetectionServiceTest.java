package com.nexabank.fraud.service;

import com.nexabank.fraud.entity.FraudAlert;
import com.nexabank.fraud.entity.FraudAlertStatus;
import com.nexabank.fraud.entity.FraudRuleType;
import com.nexabank.fraud.kafka.event.TransactionCreatedEvent;
import com.nexabank.fraud.repository.FraudAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock FraudAlertRepository fraudAlertRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks FraudDetectionService fraudDetectionService;

    private TransactionCreatedEvent event;

    @BeforeEach
    void setUp() {
        event = new TransactionCreatedEvent();
        event.setTransactionId(1L);
        event.setUserId(10L);
        event.setSourceAccountId(100L);
        event.setDestinationAccountId(200L);
        event.setAmount(new BigDecimal("500.00"));
        event.setTimestamp(LocalDateTime.now());
    }

    @Test
    void analyze_SmallTransaction_NoAlertCreated() {
        when(fraudAlertRepository.countRecentAlertsByUser(anyLong(), any())).thenReturn(0L);

        fraudDetectionService.analyze(event);

        verify(fraudAlertRepository, never()).save(any());
    }

    @Test
    void analyze_LargeTransaction_CreatesLargeTransactionAlert() {
        event.setAmount(new BigDecimal("15000.00")); // >= 10000

        when(fraudAlertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fraudDetectionService.analyze(event);

        ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository, atLeastOnce()).save(captor.capture());

        boolean hasLargeTransactionRule = captor.getAllValues().stream()
            .anyMatch(a -> a.getRuleType() == FraudRuleType.LARGE_TRANSACTION);
        assertThat(hasLargeTransactionRule).isTrue();
    }

    @Test
    void analyze_LargeTransaction_PublishesToFraudAlertsTopic() {
        event.setAmount(new BigDecimal("25000.00"));

        when(fraudAlertRepository.save(any())).thenAnswer(inv -> {
            FraudAlert a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        fraudDetectionService.analyze(event);

        verify(kafkaTemplate, atLeastOnce()).send(eq("fraud-alerts"), any());
    }

    @Test
    void analyze_RapidTransfers_CreatesRapidTransferAlert() {
        event.setAmount(new BigDecimal("100.00"));
        when(fraudAlertRepository.countRecentAlertsByUser(eq(10L), any())).thenReturn(4L); // > 3

        when(fraudAlertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fraudDetectionService.analyze(event);

        ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository, atLeastOnce()).save(captor.capture());

        boolean hasRapidRule = captor.getAllValues().stream()
            .anyMatch(a -> a.getRuleType() == FraudRuleType.RAPID_REPEATED_TRANSFERS);
        assertThat(hasRapidRule).isTrue();
    }

    @Test
    void analyze_AlertCreated_HasPendingStatus() {
        event.setAmount(new BigDecimal("50000.00"));

        when(fraudAlertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fraudDetectionService.analyze(event);

        ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository, atLeastOnce()).save(captor.capture());

        captor.getAllValues().forEach(a ->
            assertThat(a.getStatus()).isEqualTo(FraudAlertStatus.PENDING));
    }
}
