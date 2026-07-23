package com.medion.hardwarestore.domain.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByStoreId(UUID storeId);
    List<Subscription> findByAutoRenewalAndNextPaymentDateBefore(Boolean autoRenewal, LocalDateTime date);
}
