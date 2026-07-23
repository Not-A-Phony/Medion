package com.medion.hardwarestore.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentRetryLogRepository extends JpaRepository<PaymentRetryLog, UUID> {
}
