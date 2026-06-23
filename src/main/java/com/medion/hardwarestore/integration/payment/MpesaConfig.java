package com.medion.hardwarestore.integration.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "mpesa")
public class MpesaConfig {
    private String consumerKey;
    private String consumerSecret;
    private String passkey;
    private String shortcode;
    // For sandbox use "https://sandbox.safaricom.co.ke", for prod use "https://api.safaricom.co.ke"
    private String baseUrl = "https://sandbox.safaricom.co.ke";
}
