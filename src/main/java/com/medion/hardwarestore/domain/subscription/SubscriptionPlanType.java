package com.medion.hardwarestore.domain.subscription;

/**
 * Billing model for a {@link Subscription}.
 * Kept separate from the store-level {@code SubscriptionType} enum (COMMISSION/PREMIUM)
 * so existing store data and seeding are unaffected.
 */
public enum SubscriptionPlanType {
    MONTHLY_FLAT_RATE,
    COMMISSION_BASED
}
