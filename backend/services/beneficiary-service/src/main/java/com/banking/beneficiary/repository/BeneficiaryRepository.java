package com.banking.beneficiary.repository;

import com.banking.beneficiary.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    List<Beneficiary> findByUserIdAndActiveTrue(UUID userId);

    Optional<Beneficiary> findByUserIdAndAccountNumber(UUID userId, String accountNumber);

    boolean existsByUserIdAndAccountNumber(UUID userId, String accountNumber);
}
