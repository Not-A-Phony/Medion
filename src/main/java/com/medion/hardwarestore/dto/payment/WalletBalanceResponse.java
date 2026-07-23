package com.medion.hardwarestore.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletBalanceResponse(
        UUID storeId,
        BigDecimal availableBalance,
        BigDecimal lockedBalance,
        BigDecimal totalBalance,
        BigDecimal totalEarnings,
        BigDecimal totalCommissionsPaid,
        LocalDateTime lastWithdrawalDate
) {}
