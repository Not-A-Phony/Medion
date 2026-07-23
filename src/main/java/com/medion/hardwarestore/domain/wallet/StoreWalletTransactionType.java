package com.medion.hardwarestore.domain.wallet;

/**
 * Type of movement recorded on a {@link StoreWallet} via a {@link StoreWalletTransaction}.
 */
public enum StoreWalletTransactionType {
    CREDIT,
    COMMISSION_DEDUCTION,
    WITHDRAWAL,
    REFUND,
    ADJUSTMENT
}
