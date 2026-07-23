package com.medion.hardwarestore.controller;

import com.medion.hardwarestore.dto.payment.SubscriptionPlan;
import com.medion.hardwarestore.dto.payment.SubscriptionStatusResponse;
import com.medion.hardwarestore.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getPlans() {
        return ResponseEntity.ok(subscriptionService.getAvailablePlans());
    }

    @GetMapping("/status/{storeId}")
    public ResponseEntity<SubscriptionStatusResponse> getStatus(@PathVariable UUID storeId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionStatus(storeId));
    }

    @DeleteMapping("/{storeId}/cancel")
    public ResponseEntity<Map<String, String>> cancel(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String reason) {
        subscriptionService.cancelSubscription(storeId, reason);
        return ResponseEntity.ok(Map.of("status", "cancelled"));
    }
}
