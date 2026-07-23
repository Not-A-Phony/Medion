package com.medion.hardwarestore.domain.subscription;

/**
 * Lifecycle status of a {@link Subscription}.
 */
public enum SubscriptionStatus {
    ACTIVE,
    SUSPENDED,
    CANCELLED,
    EXPIRED,
    PENDING_ACTIVATION
}
