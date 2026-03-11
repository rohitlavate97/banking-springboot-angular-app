package com.banking.fraud.repository;

import com.banking.fraud.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {

    List<FraudAlert> findByUserId(UUID userId);

    @Query("""
            SELECT COUNT(fa) FROM FraudAlert fa
            WHERE fa.userId = :userId
            AND fa.createdAt >= :since
            AND fa.transactionReference IS NOT NULL
            """)
    long countRecentAlertsByUser(@Param("userId") UUID userId,
                                 @Param("since") LocalDateTime since);

    boolean existsByTransactionReference(String transactionReference);
}
