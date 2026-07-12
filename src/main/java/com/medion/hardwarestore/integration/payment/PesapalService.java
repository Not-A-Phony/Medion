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
        // Return a simulated URL pointing to our demo endpoint
        return "http://localhost:8080/api/v1/payments/demo-success?merchantReference=" + order.getId().toString();
    }

    @Override
    public PaymentStatus verifyPayment(String transactionRef) {
        log.info("Verifying Pesapal transaction {}", transactionRef);
        return PaymentStatus.SUCCESS;
    }
}
