package com.banking.transaction.repository;

import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByReferenceNumber(String referenceNumber);
    Page<Transaction> findByUserId(UUID userId, Pageable pageable);
    List<Transaction> findBySourceAccountNumberAndCreatedAtBetween(
            String accountNumber, LocalDateTime from, LocalDateTime to);
    List<Transaction> findByUserIdAndStatus(UUID userId, TransactionStatus status);
}
