package com.medion.hardwarestore.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID transactionId,
        String checkoutRequestId,
        String status,
        String message,
        BigDecimal amount,
        String mpesaReceiptNumber,
        LocalDateTime createdAt
) {}
