package com.medion.hardwarestore.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByExternalReferenceId(String externalReferenceId);
    Optional<PaymentTransaction> findByMpesaReceiptNumber(String mpesaReceiptNumber);
}
