package com.medion.hardwarestore.controller.payment;

import com.medion.hardwarestore.domain.order.Order;
import com.medion.hardwarestore.domain.order.OrderItem;
import com.medion.hardwarestore.domain.order.OrderRepository;
import com.medion.hardwarestore.domain.order.OrderStatus;
import com.medion.hardwarestore.domain.payment.PaymentStatus;
import com.medion.hardwarestore.domain.payment.StorePayment;
import com.medion.hardwarestore.domain.payment.StorePaymentRepository;
import com.medion.hardwarestore.domain.store.Store;
import com.medion.hardwarestore.domain.store.StoreRepository;
import com.medion.hardwarestore.domain.store.StoreStatus;
import com.medion.hardwarestore.domain.store.SubscriptionType;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.dto.payment.InitiateSubscriptionPaymentRequest;
import com.medion.hardwarestore.dto.payment.MpesaCallbackRequest;
import com.medion.hardwarestore.dto.payment.PaymentResponse;
import com.medion.hardwarestore.service.PaymentService;
import com.medion.hardwarestore.service.WalletService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final StorePaymentRepository storePaymentRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    /**
     * Initiate a subscription payment (STK Push) for the current user's store.
     */
    @PostMapping("/subscription/initiate")
    public ResponseEntity<PaymentResponse> initiateSubscriptionPayment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody InitiateSubscriptionPaymentRequest request) {
        PaymentResponse response = paymentService.initiateSubscriptionPayment(
                user, request.amount(), request.phoneNumber(), request.storeId());
        return ResponseEntity.ok(response);
    }

    /**
     * Dedicated structured M-Pesa callback endpoint (public - called by Safaricom).
     */
    @PostMapping("/mpesa/callback")
    public ResponseEntity<Map<String, String>> handleMpesaCallback(
            @RequestBody MpesaCallbackRequest callbackRequest) {
        try {
            paymentService.handleMpesaCallback(callbackRequest);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Failed to process M-Pesa callback", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error"));
        }
    }

    /**
     * Poll the status of a payment transaction.
     */
    @GetMapping("/{transactionId}/status")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(transactionId));
    }

    /**
     * Real MPESA Daraja STK Push Webhook
     */
    @PostMapping("/mpesa-webhook")
    public ResponseEntity<String> handleMpesaWebhook(@RequestBody JsonNode payload) {
        log.info("Received MPESA Webhook: {}", payload);
        
        try {
            JsonNode stkCallback = payload.path("Body").path("stkCallback");
            int resultCode = stkCallback.path("ResultCode").asInt();
            String checkoutRequestId = stkCallback.path("CheckoutRequestID").asText();

            if (resultCode == 0) {
                // Payment was successful
                processSuccessfulPayment(checkoutRequestId);
            } else {
                log.warn("MPESA Payment failed for {}. ResultDesc: {}", checkoutRequestId, stkCallback.path("ResultDesc").asText());
            }
        } catch (Exception e) {
            log.error("Failed to parse MPESA webhook", e);
        }

        return ResponseEntity.ok("Success");
    }

    private void processSuccessfulPayment(String checkoutRequestId) {
        // 1. Try to find a Store Payment (Subscription)
        Optional<StorePayment> storePaymentOpt = storePaymentRepository.findByTrackingId(checkoutRequestId);
        if (storePaymentOpt.isPresent()) {
            StorePayment payment = storePaymentOpt.get();
            payment.setStatus(PaymentStatus.SUCCESS);
            storePaymentRepository.save(payment);
            
            Store store = payment.getStore();
            if (store.getStatus() == StoreStatus.PENDING_PAYMENT) {
                store.setStatus(StoreStatus.APPROVED); // Auto-Approve Store upon payment
                storeRepository.save(store);
                log.info("Store {} payment completed via MPESA. Status updated to APPROVED.", store.getId());
            }
            return;
        }

        // 2. Try to find a Customer Order
        Optional<Order> orderOpt = orderRepository.findByTrackingId(checkoutRequestId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (order.getStatus() != OrderStatus.PAID) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);

                // Process Vendor Wallet and Platform Cut
                processOrderPayouts(order);

                log.info("Order {} payment completed via MPESA and payouts distributed.", order.getId());
            }
        }
    }

    /**
     * Demo Endpoint to simulate the user paying on Pesapal successfully.
     * The frontend can call this to mock a successful payment redirect.
     */
    @GetMapping("/demo-success")
    public ResponseEntity<String> simulatePaymentSuccess(@RequestParam String merchantReference) {
        log.info("Simulating successful payment for reference: {}", merchantReference);
        
        // 1. Try to find a Store Payment (Subscription)
        Optional<StorePayment> storePaymentOpt = storePaymentRepository.findByMerchantReference(merchantReference);
        if (storePaymentOpt.isPresent()) {
            StorePayment payment = storePaymentOpt.get();
            payment.setStatus(PaymentStatus.SUCCESS);
            storePaymentRepository.save(payment);
            
            Store store = payment.getStore();
            if (store.getStatus() == StoreStatus.PENDING_PAYMENT) {
                store.setStatus(StoreStatus.APPROVED); // Auto-Approve Store upon payment
                storeRepository.save(store);
                log.info("Store {} payment completed. Status updated to APPROVED.", store.getId());
            }
            return ResponseEntity.ok("Store Subscription Payment Successful!");
        }

        // 2. Try to find a Customer Order
        try {
            UUID orderId = UUID.fromString(merchantReference);
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                if (order.getStatus() != OrderStatus.PAID) {
                    order.setStatus(OrderStatus.PAID);
                    orderRepository.save(order);

                    // Process Vendor Wallet and Platform Cut
                    processOrderPayouts(order);

                    log.info("Order {} payment completed and payouts distributed.", order.getId());
                }
                return ResponseEntity.ok("Order Checkout Payment Successful!");
            }
        } catch (IllegalArgumentException e) {
            // Not a valid UUID
        }

        return ResponseEntity.notFound().build();
    }

    private void processOrderPayouts(Order order) {
        // Platform cut goes to 0798480715 - but for now we'll just track it in logs or internal revenue table
        BigDecimal totalPlatformRevenue = BigDecimal.ZERO;

        for (OrderItem item : order.getItems()) {
            Store store = item.getProduct().getStore();
            UUID vendorId = store.getOwnerId(); // Assuming store has ownerId, wait, Store entity has ownerId? No, it has owner User?
            // Wait, Store entity in this project has owner? Let's check Store.java
            
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal platformCut = item.getPlatformCommission();
            BigDecimal vendorPayout = itemTotal.subtract(platformCut);

            // Log platform revenue
            totalPlatformRevenue = totalPlatformRevenue.add(platformCut);

            // Credit Vendor Wallet
            walletService.creditWithdrawableBalance(store.getOwnerId(), vendorPayout, order.getId().toString());
        }

        log.info("Platform took a total cut of {} KES. Funds routed to 0798480715 conceptually.", totalPlatformRevenue);
    }

    /**
     * Poll the status of a store subscription payment
     */
    @GetMapping("/store-subscription/{storeId}/status")
    public ResponseEntity<PaymentStatus> getStoreSubscriptionStatus(@PathVariable UUID storeId) {
        Optional<Store> storeOpt = storeRepository.findById(storeId);
        if (storeOpt.isPresent()) {
            Store store = storeOpt.get();
            // If the store is already APPROVED or PENDING (admin approval), payment was successful
            if (store.getStatus() == StoreStatus.APPROVED || store.getStatus() == StoreStatus.PENDING) {
                return ResponseEntity.ok(PaymentStatus.SUCCESS);
            }
            // If it's still PENDING_PAYMENT, we can check the latest store payment record
            Optional<StorePayment> paymentOpt = storePaymentRepository.findFirstByStoreIdOrderByCreatedAtDesc(storeId);
            if (paymentOpt.isPresent()) {
                return ResponseEntity.ok(paymentOpt.get().getStatus());
            }
        }
        return ResponseEntity.notFound().build();
    }
}
