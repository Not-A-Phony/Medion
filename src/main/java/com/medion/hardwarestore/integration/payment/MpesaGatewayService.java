package com.medion.hardwarestore.integration.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medion.hardwarestore.exception.MpesaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpesaGatewayService {

    @Value("${mpesa.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${mpesa.passkey}")
    private String passkey;

    @Value("${mpesa.shortcode}")
    private String shortcode;

    @Value("${mpesa.callback-url:https://medion-6dal.onrender.com/api/v1/payments/mpesa-webhook}")
    private String callbackUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String OAUTH_URL = "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials";
    private static final String STK_PUSH_URL = "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest";

    /**
     * Get M-Pesa access token for API authentication.
     * Validates credentials are configured before attempting OAuth.
     *
     * @return Access token for M-Pesa API calls
     * @throws MpesaException if credentials missing or OAuth fails
     */
    public String getAccessToken() {
        try {
            // Validate credentials are configured (CLAUDE.md spec §4)
            if (consumerKey == null || consumerKey.isEmpty()) {
                throw new MpesaException("M-Pesa consumer key not configured. Set mpesa.consumer-key in environment");
            }
            if (consumerSecret == null || consumerSecret.isEmpty()) {
                throw new MpesaException("M-Pesa consumer secret not configured. Set mpesa.consumer-secret in environment");
            }

            String credentials = consumerKey + ":" + consumerSecret;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(encodedCredentials);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Requesting M-Pesa access token from OAuth endpoint");
            ResponseEntity<Map> response = restTemplate.exchange(OAUTH_URL, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object accessToken = response.getBody().get("access_token");
                if (accessToken != null && !accessToken.toString().isEmpty()) {
                    log.debug("M-Pesa access token generated successfully");
                    return (String) accessToken;
                }
                throw new MpesaException("M-Pesa OAuth returned empty access token");
            }
            throw new MpesaException("M-Pesa OAuth failed with status: " + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("Network error connecting to M-Pesa OAuth endpoint", e);
            throw new MpesaException("Cannot reach M-Pesa service. Check your internet connection and try again.", e);
        } catch (MpesaException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error generating M-Pesa access token", e);
            throw new MpesaException("Failed to generate M-Pesa access token: " + e.getMessage(), e);
        }
    }

    /**
     * Initiate STK Push to user's phone for M-Pesa payment.
     * Validates all inputs and configuration before API call (CLAUDE.md spec §4).
     *
     * @param phoneNumber User's phone number (e.g., 0712345678 or 254712345678)
     * @param amount Payment amount in KES (must be > 0)
     * @param accountReference Order/transaction reference ID
     * @return CheckoutRequestID from M-Pesa for tracking
     * @throws MpesaException if validation fails or M-Pesa API returns error
     */
    public String initiateStkPush(String phoneNumber, long amount, String accountReference) {
        // Validate inputs before any processing
        validatePaymentInputs(phoneNumber, amount, accountReference);

        // Validate M-Pesa configuration
        validateMpesaConfig();

        try {
            // Get access token (will throw MpesaException if fails)
            String token = getAccessToken();

            // Format and validate phone number (throws MpesaException if invalid)
            String formattedPhone = formatPhoneNumber(phoneNumber);
            log.debug("Formatted phone number: {} (original: {})", formattedPhone, phoneNumber);

            // Generate password and timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = Base64.getEncoder().encodeToString(
                    (shortcode + passkey + timestamp).getBytes(StandardCharsets.UTF_8)
            );

            // Build STK Push request payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("BusinessShortCode", shortcode);
            payload.put("Password", password);
            payload.put("Timestamp", timestamp);
            payload.put("TransactionType", "CustomerPayBillOnline");
            payload.put("Amount", amount);
            payload.put("PartyA", formattedPhone);
            payload.put("PartyB", shortcode);
            payload.put("PhoneNumber", formattedPhone);
            payload.put("CallBackURL", callbackUrl);
            payload.put("AccountReference", accountReference);
            payload.put("TransactionDesc", "Hardware Store Payment");

            // Prepare HTTP request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            log.info("Initiating M-Pesa STK Push: amount={} KES, phone={}, reference={}",
                    amount, formattedPhone, accountReference);

            // Call M-Pesa STK Push endpoint
            ResponseEntity<Map> response = restTemplate.postForEntity(STK_PUSH_URL, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String checkoutRequestId = (String) response.getBody().get("CheckoutRequestID");
                String responseCode = (String) response.getBody().get("ResponseCode");

                // Check if M-Pesa accepted the request (response code "0" = success)
                if ("0".equals(responseCode) && checkoutRequestId != null && !checkoutRequestId.isEmpty()) {
                    log.info("STK Push sent successfully. CheckoutRequestID: {}, Reference: {}",
                            checkoutRequestId, accountReference);
                    return checkoutRequestId;
                }

                // M-Pesa rejected the request
                String errorMessage = (String) response.getBody().get("ResponseDescription");
                log.warn("M-Pesa rejected STK Push. Code: {}, Message: {}", responseCode, errorMessage);
                throw new MpesaException("M-Pesa rejected payment request: " + errorMessage);
            }

            // Unexpected response status
            log.error("M-Pesa STK Push endpoint returned status: {}", response.getStatusCode());
            throw new MpesaException("M-Pesa service error. Status: " + response.getStatusCode());

        } catch (RestClientException e) {
            log.error("Network error connecting to M-Pesa STK Push endpoint", e);
            throw new MpesaException("Cannot reach M-Pesa service. Please check your internet connection and try again.", e);
        } catch (MpesaException e) {
            throw e;  // Re-throw MpesaException as-is
        } catch (Exception e) {
            log.error("Unexpected error during STK Push initiation", e);
            throw new MpesaException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validate payment input parameters.
     *
     * @throws MpesaException if any validation fails
     */
    private void validatePaymentInputs(String phoneNumber, long amount, String accountReference) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new MpesaException("Phone number is required for M-Pesa payment");
        }
        if (amount <= 0) {
            throw new MpesaException("Payment amount must be greater than 0 KES");
        }
        if (accountReference == null || accountReference.trim().isEmpty()) {
            throw new MpesaException("Account reference (Order/Transaction ID) is required");
        }
    }

    /**
     * Validate M-Pesa configuration is complete.
     *
     * @throws MpesaException if any configuration is missing
     */
    private void validateMpesaConfig() {
        if (shortcode == null || shortcode.isEmpty()) {
            throw new MpesaException("M-Pesa shortcode not configured. Set mpesa.shortcode in environment");
        }
        if (passkey == null || passkey.isEmpty()) {
            throw new MpesaException("M-Pesa passkey not configured. Set mpesa.passkey in environment");
        }
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            throw new MpesaException("M-Pesa callback URL not configured. Set mpesa.callback-url in environment");
        }
    }

    /**
     * Format phone number to M-Pesa standard format (254XXXXXXXXX).
     * Validates format and throws descriptive error if invalid.
     *
     * @param phone Phone number in any format (0712345678, 254712345678, +254712345678, etc.)
     * @return Formatted phone number (254XXXXXXXXX)
     * @throws MpesaException if phone number format is invalid
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new MpesaException("Phone number cannot be empty");
        }

        // Remove all non-digit characters
        String digits = phone.replaceAll("[^0-9]", "");

        if (digits.isEmpty()) {
            throw new MpesaException("Phone number must contain at least one digit. Got: " + phone);
        }

        // Convert to Safaricom Daraja format (254XXXXXXXXX)
        String formatted;
        if (digits.startsWith("254")) {
            formatted = digits;
        } else if (digits.startsWith("0")) {
            formatted = "254" + digits.substring(1);
        } else if (digits.startsWith("7") || digits.startsWith("1")) {
            formatted = "254" + digits;
        } else {
            formatted = "254" + digits;
        }

        // Validate final format
        if (formatted.length() < 12 || formatted.length() > 13) {
            throw new MpesaException(
                    "Invalid phone number format. Expected: 0712345678 or 254712345678. Got: " + phone +
                    " (formatted as " + formatted + ", length: " + formatted.length() + ")"
            );
        }

        // Validate it's a Kenyan M-Pesa number (254 7xxx or 254 1xxx)
        if (!formatted.startsWith("2547") && !formatted.startsWith("2541")) {
            throw new MpesaException(
                    "Invalid phone number. Must be a Kenyan M-Pesa number (starting with 2547 or 2541). Got: " + formatted
            );
        }

        return formatted;
    }
}
