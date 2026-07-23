package com.medion.hardwarestore.domain.subscription;

/**
 * Status of a single {@link SubscriptionPayment} billing record.
 */
public enum SubscriptionPaymentStatus {
    PENDING,
    PAID,
    OVERDUE,
    FAILED,
    CANCELLED
}
