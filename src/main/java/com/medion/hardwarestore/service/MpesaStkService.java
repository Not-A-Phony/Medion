package com.medion.hardwarestore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medion.hardwarestore.domain.payment.PaymentRetryLog;
import com.medion.hardwarestore.domain.payment.PaymentRetryLogRepository;
import com.medion.hardwarestore.exception.MpesaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * M-Pesa STK Push orchestration for payment-transaction driven flows
 * (subscriptions, etc). Complements the existing MPesaService/MpesaGatewayService
 * used by the order flow; this one caches the checkout->transaction mapping and the
 * access token via {@link PaymentCacheService} and records retry logs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpesaStkService {

    @Value("${mpesa.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${mpesa.passkey}")
    private String passkey;

    @Value("${mpesa.shortcode}")
    private String shortcode;

    @Value("${mpesa.is-live:false}")
    private boolean isLive;

    @Value("${mpesa.stk-callback-url:https://medion-6dal.onrender.com/api/v1/payments/mpesa/callback}")
    private String callbackUrl;

    private final PaymentRetryLogRepository retryLogRepository;
    private final PaymentCacheService cacheService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SANDBOX_BASE = "https://sandbox.safaricom.co.ke";
    private static final String LIVE_BASE = "https://api.safaricom.co.ke";
    private static final String AUTH_PATH = "/oauth/v1/generate?grant_type=client_credentials";
    private static final String STK_PUSH_PATH = "/mpesa/stkpush/v1/processrequest";
    private static final String ACCESS_TOKEN_CACHE_KEY = "mpesa:access_token";
    private static final String TXN_CACHE_PREFIX = "mpesa:transaction:";

    private String baseUrl() {
        return isLive ? LIVE_BASE : SANDBOX_BASE;
    }

    /**
     * Initiate an STK Push and return the CheckoutRequestID.
     * Caches checkoutRequestId -> paymentTransactionId for 10 minutes.
     */
    public String initiateStkPush(String phoneNumber, BigDecimal amount, String accountReference,
                                  UUID paymentTransactionId) {
        String token = getAccessToken();
        String timestamp = generateTimestamp();
        String password = generatePassword(timestamp);
        String formattedPhone = formatPhoneNumber(phoneNumber);

        Map<String, Object> payload = new HashMap<>();
        payload.put("BusinessShortCode", shortcode);
        payload.put("Password", password);
        payload.put("Timestamp", timestamp);
        payload.put("TransactionType", "CustomerPayBillOnline");
        payload.put("Amount", amount.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString());
        payload.put("PartyA", formattedPhone);
        payload.put("PartyB", shortcode);
        payload.put("PhoneNumber", formattedPhone);
        payload.put("CallBackURL", callbackUrl);
        payload.put("AccountReference", accountReference);
        payload.put("TransactionDesc", "Medion subscription payment");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl() + STK_PUSH_PATH, new HttpEntity<>(payload, headers), Map.class);

            Map body = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && body != null
                    && "0".equals(String.valueOf(body.get("ResponseCode")))) {
                String checkoutRequestId = (String) body.get("CheckoutRequestID");
                cacheTransaction(paymentTransactionId, checkoutRequestId);
                log.info("STK Push initiated for transaction {} (checkout {})",
                        paymentTransactionId, checkoutRequestId);
                return checkoutRequestId;
            }

            String responseBody = safeSerialize(body);
            recordRetryLog(paymentTransactionId, responseBody,
                    body != null ? String.valueOf(body.get("ResponseCode")) : "UNKNOWN",
                    body != null ? String.valueOf(body.get("ResponseDescription")) : "No response body");
            throw new MpesaException("STK Push was not accepted by M-Pesa");
        } catch (MpesaException e) {
            throw e;
        } catch (Exception e) {
            log.error("STK Push failed for transaction {}", paymentTransactionId, e);
            recordRetryLog(paymentTransactionId, e.getMessage(), "EXCEPTION", e.getMessage());
            throw new MpesaException("Failed to initiate STK Push", e);
        }
    }

    /**
     * Fetch (and cache for 3500s) the OAuth access token.
     */
    public String getAccessToken() {
        Optional<String> cached = cacheService.get(ACCESS_TOKEN_CACHE_KEY);
        if (cached.isPresent()) {
            return cached.get();
        }

        try {
            String credentials = consumerKey + ":" + consumerSecret;
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl() + AUTH_PATH, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String token = (String) response.getBody().get("access_token");
                cacheService.put(ACCESS_TOKEN_CACHE_KEY, token, Duration.ofSeconds(3500));
                return token;
            }
            throw new MpesaException("Empty access token response from M-Pesa");
        } catch (MpesaException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to obtain M-Pesa access token", e);
            throw new MpesaException("Failed to obtain M-Pesa access token", e);
        }
    }

    /**
     * Validate that a callback signature matches the SHA-256/base64 hash of the body.
     */
    public boolean validateCallbackSignature(String signature, String body) {
        if (signature == null || body == null) {
            return false;
        }
        return signature.equals(generateCallbackSignature(body));
    }

    public String generateCallbackSignature(String body) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new MpesaException("Failed to generate callback signature", e);
        }
    }

    public String generatePassword() {
        return generatePassword(generateTimestamp());
    }

    private String generatePassword(String timestamp) {
        String str = shortcode + passkey + timestamp;
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    public String generateTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    public void cacheTransaction(UUID paymentTransactionId, String checkoutRequestId) {
        cacheService.put(TXN_CACHE_PREFIX + checkoutRequestId,
                paymentTransactionId.toString(), Duration.ofMinutes(10));
    }

    /**
     * Look up the payment transaction id previously cached against a checkout request id.
     */
    public Optional<UUID> resolveTransactionId(String checkoutRequestId) {
        return cacheService.get(TXN_CACHE_PREFIX + checkoutRequestId).map(UUID::fromString);
    }

    public void recordRetryLog(UUID paymentTransactionId, String responseBody,
                               String errorCode, String errorMessage) {
        PaymentRetryLog retryLog = PaymentRetryLog.builder()
                .paymentTransactionId(paymentTransactionId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .responseBody(responseBody)
                .build();
        retryLogRepository.save(retryLog);
    }

    private String safeSerialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String formatPhoneNumber(String phone) {
        phone = phone.replaceAll("[^0-9]", "");
        if (phone.startsWith("0")) {
            return "254" + phone.substring(1);
        } else if (phone.startsWith("7") || phone.startsWith("1")) {
            return "254" + phone;
        }
        return phone;
    }
}
