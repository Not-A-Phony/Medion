package com.medion.hardwarestore.integration.payment;

import com.medion.hardwarestore.domain.order.Order;
import com.medion.hardwarestore.domain.payment.PaymentProvider;
import com.medion.hardwarestore.domain.payment.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class MPesaService implements PaymentProcessor {

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MPESA;
    }

    @Override
    public String initiatePayment(Order order, String phoneNumber) {
        log.info("Initiating MPESA payment for order {} with amount {} to phone {}", 
                 order.getId(), order.getTotalAmount(), phoneNumber);
        // Stub: In reality, call Daraja API (STK Push) here
        return "MPESA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public PaymentStatus verifyPayment(String transactionRef) {
        log.info("Verifying MPESA transaction {}", transactionRef);
        // Stub: Always return SUCCESS for now
        return PaymentStatus.SUCCESS;
    }
}
