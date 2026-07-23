package com.medion.hardwarestore.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubscriptionStatusResponse(
        boolean hasSubscription,
        String status,
        String subscriptionPlanType,
        LocalDateTime currentBillingCycleStart,
        LocalDateTime currentBillingCycleEnd,
        LocalDateTime nextPaymentDate,
        LocalDateTime lastPaymentDate,
        Boolean autoRenewal,
        BigDecimal monthlyFlatRate,
        BigDecimal commissionPercentage
) {
    public static SubscriptionStatusResponse none() {
        return new SubscriptionStatusResponse(false, null, null, null, null, null, null, null, null, null);
    }
}
