package com.medion.hardwarestore.integration.payment;

import com.medion.hardwarestore.domain.order.Order;
import com.medion.hardwarestore.domain.payment.PaymentProvider;
import com.medion.hardwarestore.domain.payment.PaymentStatus;

public interface PaymentProcessor {
    PaymentProvider getProvider();
    String initiatePayment(Order order, String phoneNumber);
    PaymentStatus verifyPayment(String transactionRef);
}
