package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.payment.PaymentStatus;
import com.medion.hardwarestore.domain.payment.StorePayment;
import com.medion.hardwarestore.domain.payment.StorePaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorePesapalService {

    private final StorePaymentRepository storePaymentRepository;

    /**
     * Mocks submitting an order to Pesapal and returns a payment URL.
     */
    public String submitOrder(StorePayment payment) {
        log.info("Mock submitting order to Pesapal for Merchant Reference: {}", payment.getMerchantReference());
        // In a real integration, we would call Pesapal API here
        // and extract the redirect_url from the response.
        
        // Return a mock redirect URL
        return "https://cybqa.pesapal.com/pesapaliframe/PesapalIframe3/Index?OrderTrackingId=" + UUID.randomUUID().toString();
    }
    
    /**
     * Mocks querying the transaction status from Pesapal.
     */
    public PaymentStatus getTransactionStatus(String trackingId) {
        log.info("Mock fetching transaction status from Pesapal for Tracking ID: {}", trackingId);
        // In a real integration, call Pesapal to get the actual status.
        return PaymentStatus.SUCCESS;
    }
}
