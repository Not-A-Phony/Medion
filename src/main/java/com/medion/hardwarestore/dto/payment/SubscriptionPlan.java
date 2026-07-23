package com.medion.hardwarestore.dto.payment;

import java.math.BigDecimal;
import java.util.List;

public record SubscriptionPlan(
        String subscriptionPlanType,
        String name,
        String description,
        BigDecimal monthlyFlatRate,
        BigDecimal commissionPercentage,
        List<String> features
) {}
