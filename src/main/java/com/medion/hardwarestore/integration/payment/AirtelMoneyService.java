package com.medion.hardwarestore.integration.payment;

import com.medion.hardwarestore.domain.order.Order;
import com.medion.hardwarestore.domain.payment.PaymentProvider;
import com.medion.hardwarestore.domain.payment.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class AirtelMoneyService implements PaymentProcessor {

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.AIRTEL_MONEY;
    }

    @Override
    public String initiatePayment(Order order, String phoneNumber) {
        log.info("Initiating Airtel Money payment for order {} with amount {} to phone {}", 
                 order.getId(), order.getTotalAmount(), phoneNumber);
        // Stub
        return "AIRTEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public PaymentStatus verifyPayment(String transactionRef) {
        log.info("Verifying Airtel Money transaction {}", transactionRef);
        return PaymentStatus.SUCCESS;
    }
}
