package com.medion.hardwarestore.integration.payment;

import com.medion.hardwarestore.domain.order.Order;
import com.medion.hardwarestore.domain.payment.PaymentProvider;
import com.medion.hardwarestore.domain.payment.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class PesapalService implements PaymentProcessor {

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.PESAPAL;
    }

    @Override
    public String initiatePayment(Order order, String phoneNumber) {
        log.info("Initiating Pesapal payment for order {} with amount {}", order.getId(), order.getTotalAmount());
        // Stub: Generate Iframe link or redirect URL
        return "PESAPAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public PaymentStatus verifyPayment(String transactionRef) {
        log.info("Verifying Pesapal transaction {}", transactionRef);
        return PaymentStatus.SUCCESS;
    }
}
