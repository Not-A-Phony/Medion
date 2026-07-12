package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.category.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CommissionEngine {

    private final PlatformConfigService configService;

    public BigDecimal calculateCommission(BigDecimal totalAmount, Category category) {
        BigDecimal rate = getCommissionRate(category);
        return totalAmount.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateVendorEarnings(BigDecimal totalAmount, Category category) {
        return totalAmount.subtract(calculateCommission(totalAmount, category));
    }

    private BigDecimal getCommissionRate(Category category) {
        if (category != null && category.getCommissionRate() != null) {
            return category.getCommissionRate();
        }
        return configService.getDefaultCommissionRate();
    }
}
