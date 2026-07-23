package com.medion.hardwarestore.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record WithdrawalRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String bankAccountNumber
) {}
