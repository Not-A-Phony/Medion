package com.medion.hardwarestore.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface StorePaymentRepository extends JpaRepository<StorePayment, UUID> {
    Optional<StorePayment> findByMerchantReference(String merchantReference);
    Optional<StorePayment> findByTrackingId(String trackingId);
}
