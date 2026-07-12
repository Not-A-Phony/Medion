package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.config.PlatformConfig;
import com.medion.hardwarestore.domain.config.PlatformConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlatformConfigService {

    private final PlatformConfigRepository configRepository;

    public static final String DEFAULT_COMMISSION_RATE = "DEFAULT_COMMISSION_RATE";

    public BigDecimal getDefaultCommissionRate() {
        return configRepository.findByConfigKey(DEFAULT_COMMISSION_RATE)
                .map(config -> new BigDecimal(config.getConfigValue()))
                .orElse(new BigDecimal("5.00")); // Fallback
    }

    public void setDefaultCommissionRate(BigDecimal rate) {
        Optional<PlatformConfig> existing = configRepository.findByConfigKey(DEFAULT_COMMISSION_RATE);
        if (existing.isPresent()) {
            PlatformConfig config = existing.get();
            config.setConfigValue(rate.toString());
            configRepository.save(config);
        } else {
            configRepository.save(PlatformConfig.builder()
                    .configKey(DEFAULT_COMMISSION_RATE)
                    .configValue(rate.toString())
                    .description("Global default commission rate for marketplace")
                    .build());
        }
    }
}
