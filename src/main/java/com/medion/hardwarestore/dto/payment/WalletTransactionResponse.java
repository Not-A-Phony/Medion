package com.medion.hardwarestore.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id,
        BigDecimal amount,
        String transactionType,
        String description,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        LocalDateTime createdAt
) {}
