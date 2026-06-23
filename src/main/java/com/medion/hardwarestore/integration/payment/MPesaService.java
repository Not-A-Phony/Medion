package com.medion.hardwarestore.integration.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medion.hardwarestore.domain.order.Order;
import com.medion.hardwarestore.domain.payment.PaymentProvider;
import com.medion.hardwarestore.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class MPesaService implements PaymentProcessor {

    private final MpesaConfig config;
    private final RestClient restClient = RestClient.create();

    record OAuthResponse(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") String expiresIn) {}
    record StkPushRequest(
            String BusinessShortCode, String Password, String Timestamp, String TransactionType,
            String Amount, String PartyA, String PartyB, String PhoneNumber,
            String CallBackURL, String AccountReference, String TransactionDesc
    ) {}
    record StkPushResponse(String CheckoutRequestID, String ResponseCode, String ResponseDescription, String CustomerMessage) {}

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MPESA;
    }

    @Override
    public String initiatePayment(Order order, String phoneNumber) {
        log.info("Initiating MPESA payment for order {} with amount {} to phone {}", order.getId(), order.getTotalAmount(), phoneNumber);
        
        try {
            String token = generateOAuthToken();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = generatePassword(timestamp);

            // Format phone number to 254...
            String formattedPhone = formatPhoneNumber(phoneNumber);

            StkPushRequest request = new StkPushRequest(
                    config.getShortcode(),
                    password,
                    timestamp,
                    "CustomerPayBillOnline",
                    String.valueOf(order.getTotalAmount().intValue()), // amount as string integer
                    formattedPhone,
                    config.getShortcode(),
                    formattedPhone,
                    "https://your-render-domain.onrender.com/api/v1/payments/mpesa/callback", // Replace with your live URL
                    order.getId().toString(),
                    "Payment for Order " + order.getId()
            );

            StkPushResponse response = restClient.post()
                    .uri(config.getBaseUrl() + "/mpesa/stkpush/v1/processrequest")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(StkPushResponse.class);

            if (response != null && "0".equals(response.ResponseCode())) {
                return response.CheckoutRequestID();
            } else {
                throw new RuntimeException("Failed to initiate STK Push: " + (response != null ? response.CustomerMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            log.error("MPESA STK Push failed", e);
            throw new RuntimeException("Payment initiation failed", e);
        }
    }

    @Override
    public PaymentStatus verifyPayment(String transactionRef) {
        // Normally, verification is handled by receiving the callback at the CallBackURL.
        // For querying status manually, Daraja has a separate STK Push Query API.
        log.info("Verifying MPESA transaction {}", transactionRef);
        return PaymentStatus.PENDING; // Must wait for callback
    }

    private String generateOAuthToken() {
        String credentials = config.getConsumerKey() + ":" + config.getConsumerSecret();
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        OAuthResponse response = restClient.get()
                .uri(config.getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                .retrieve()
                .body(OAuthResponse.class);

        return response.accessToken();
    }

    private String generatePassword(String timestamp) {
        String str = config.getShortcode() + config.getPasskey() + timestamp;
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    private String formatPhoneNumber(String phone) {
        // Ensure phone starts with 254 (assuming Kenyan format for M-Pesa)
        if (phone.startsWith("0")) return "254" + phone.substring(1);
        if (phone.startsWith("+")) return phone.substring(1);
        return phone;
    }
}
