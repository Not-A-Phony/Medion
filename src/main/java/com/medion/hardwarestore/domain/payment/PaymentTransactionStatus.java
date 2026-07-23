package com.medion.hardwarestore.domain.payment;

/**
 * Lifecycle status of a {@link PaymentTransaction}.
 * Distinct from the legacy {@link PaymentStatus} enum used by Payment/StorePayment.
 */
public enum PaymentTransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED,
    REFUNDED
}
