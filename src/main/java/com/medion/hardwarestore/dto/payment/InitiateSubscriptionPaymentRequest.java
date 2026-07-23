package com.medion.hardwarestore.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiateSubscriptionPaymentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String phoneNumber,
        @NotNull UUID storeId
) {}
