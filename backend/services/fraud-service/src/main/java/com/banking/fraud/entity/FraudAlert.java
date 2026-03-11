package com.banking.fraud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String transactionReference;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal transactionAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudRuleType ruleTriggered;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudAlertStatus status;

    @Column(nullable = false)
    private String description;

    @Column
    private String reviewedBy;

    @Column
    private LocalDateTime reviewedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = FraudAlertStatus.OPEN;
    }
}
