package com.medion.hardwarestore.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WithdrawalResponse(
        UUID withdrawalId,
        String status,
        BigDecimal amount,
        LocalDateTime estimatedCompletionTime
) {}
