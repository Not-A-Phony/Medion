package com.medion.hardwarestore.dto.payment;

/**
 * Simplified M-Pesa callback body used by the dedicated callback endpoint.
 * The raw Daraja webhook (nested JSON) continues to be handled separately.
 */
public record MpesaCallbackRequest(
        String checkoutRequestID,
        Integer resultCode,
        String resultDesc,
        String mpesaReceiptNumber,
        String transactionId
) {}
