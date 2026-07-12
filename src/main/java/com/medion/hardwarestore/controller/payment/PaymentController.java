package com.medion.hardwarestore.controller.payment;

import com.medion.hardwarestore.domain.payment.PaymentStatus;
import com.medion.hardwarestore.domain.payment.StorePayment;
import com.medion.hardwarestore.domain.payment.StorePaymentRepository;
import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.store.StoreRepository;
import com.medion.hardwarestore.domain.store.StoreStatus;
import com.medion.hardwarestore.service.StorePesapalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final StorePaymentRepository storePaymentRepository;
    private final StoreRepository storeRepository;
    private final StorePesapalService pesapalService;

    @PostMapping("/ipn")
    public ResponseEntity<String> handleIpn(@RequestBody Map<String, String> payload) {
        log.info("Received Pesapal IPN with payload: {}", payload);
        
        String trackingId = payload.get("OrderTrackingId");
        String merchantReference = payload.get("OrderMerchantReference");
        
        if (trackingId == null || merchantReference == null) {
            return ResponseEntity.badRequest().body("Missing required parameters");
        }
        
        StorePayment payment = storePaymentRepository.findByMerchantReference(merchantReference)
                .orElse(null);
                
        if (payment == null) {
            log.error("Payment not found for merchant reference: {}", merchantReference);
            return ResponseEntity.notFound().build();
        }
        
        // Query Pesapal for actual status
        PaymentStatus status = pesapalService.getTransactionStatus(trackingId);
        
        payment.setTrackingId(trackingId);
        payment.setStatus(status);
        storePaymentRepository.save(payment);
        
        if (status == PaymentStatus.SUCCESS) {
            Store store = payment.getStore();
            if (store.getStatus() == StoreStatus.PENDING_PAYMENT) {
                store.setStatus(StoreStatus.PENDING);
                storeRepository.save(store);
                log.info("Store {} payment completed. Status updated to PENDING.", store.getId());
            }
        }
        
        return ResponseEntity.ok("IPN processed successfully");
    }
}
